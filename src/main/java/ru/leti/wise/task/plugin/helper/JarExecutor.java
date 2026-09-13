package ru.leti.wise.task.plugin.helper;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import io.grpc.Status;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.util.JsonUtils;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.configuration.props.DockerProperties;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.PluginExecutionException;
import ru.leti.wise.task.plugin.mapper.GraphMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class JarExecutor {
    private final DockerProperties properties;
    private final DockerClient dockerClient;
    private final GraphMapper graphMapper;
    @Value("${spring.plugin.temp-folder}")
    private String tempFolder;
    private Path tempFolderPath;
    String containerBaseDir = "/app";
    @Value("${spring.plugin.runner}")
    private Resource runnerJar;

    private final Pattern exceptionRegex = Pattern.compile("exception", Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void postConstruct() {
        tempFolderPath = Paths.get(tempFolder).toAbsolutePath().normalize();
    }

    @SneakyThrows
    public String executeJar(PluginEntity plugin, PluginOuterClass.Solution solution) {
        String containerId = null;
        Path pluginPath = null;
        Path runnerPath = null;
        Path graphPath = null;
        try {
            if (solution.getPayloadCase() != PluginOuterClass.Solution.PayloadCase.GRAPH) {
                throw new BusinessException(Status.INVALID_ARGUMENT, "Плагин не поддерживает графы");
            }
            if (!Files.exists(tempFolderPath)) {
                Files.createDirectories(tempFolderPath);
            }
            var graph = graphMapper.toGraph(solution.getGraph());
            var randomId = UUID.randomUUID();
            var pluginClassName = plugin.getJarName();
            var pluginName = pluginClassName + "-" + randomId + ".jar";
            var jsonGraphFileName = "graph-" + randomId + ".json";
            var runnerFileName = "runner.jar";
            pluginPath = tempFolderPath.resolve(pluginName);
            runnerPath = tempFolderPath.resolve(runnerFileName);
            graphPath = tempFolderPath.resolve(jsonGraphFileName);
            Files.write(pluginPath, plugin.getJarFile());
            Files.copy(runnerJar.getInputStream(), runnerPath);
            if (!Files.exists(runnerPath)) {
                throw new BusinessException(Status.INTERNAL, "runner.jar not found: " + runnerPath);
            }
            var jsonGraph = JsonUtils.serializeGraph(graph);
            Files.writeString(graphPath, jsonGraph);
            var bind = new Bind(tempFolderPath.toAbsolutePath().toString(), new Volume(containerBaseDir));
            var container = dockerClient.createContainerCmd("eclipse-temurin:25-jre-alpine")
                    .withCmd("sh", "-c",
                            "java -cp " + containerBaseDir + "/" + runnerFileName + " ru.leti.wise.task.Runner " +
                                    containerBaseDir + "/" + pluginName + " " +
                                    pluginClassName + " " +
                                    containerBaseDir + "/" + jsonGraphFileName + " "
                    )
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory(properties.memoryUsageMb() * 1024 * 1024)
                            .withCpuQuota(properties.cpuQuota())
                            .withReadonlyRootfs(false)
                            .withNetworkMode("none")
                            .withBinds(bind)
                    ).exec();
            containerId = container.getId();
            var callback = new WaitContainerResultCallback();
            dockerClient.startContainerCmd(containerId)
                    .exec();
            dockerClient.waitContainerCmd(container.getId())
                    .exec(callback);
            String result;
            try {
                var exitCode = callback.awaitStatusCode(properties.containerWorkTimeout().getSeconds(), TimeUnit.SECONDS);
                List<String> logsBuilder = new ArrayList<>();
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withTailAll()
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                logsBuilder.add(new String(frame.getPayload()));
                            }
                        })
                        .awaitCompletion(5, TimeUnit.SECONDS);
                log.info("Container exited with: {}", exitCode.toString());

                String logs = String.join("\n", logsBuilder);
                if(exceptionRegex.matcher(logs).find()){
                    throw new PluginExecutionException(logs);
                }
                result = logsBuilder.getLast();
            } catch (DockerClientException e) {
                dockerClient.stopContainerCmd(container.getId()).withTimeout(5).exec();
                throw new BusinessException(Status.DEADLINE_EXCEEDED,
                        "Время выполнения плагина превышино. " +
                                "Время выполнения ограничено до " + properties.containerWorkTimeout());
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(Status.INTERNAL, e.getMessage());
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                } catch (Exception e) {
                    log.warn("Failed to remove container: {}", containerId, e);
                }
            }

            try {
                if (pluginPath != null) Files.deleteIfExists(pluginPath);
                if (runnerPath != null) Files.deleteIfExists(runnerPath);
                if (graphPath != null) Files.deleteIfExists(graphPath);
            } catch (Exception e) {
                log.warn("Failed to delete temporary files", e);
            }
        }
    }
}
