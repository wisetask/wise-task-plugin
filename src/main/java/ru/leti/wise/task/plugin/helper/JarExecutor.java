package ru.leti.wise.task.plugin.helper;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
    String containerBaseDir = "/tmp";
    @Value("${spring.plugin.runner}")
    private Resource runnerJar;

    private final Pattern exceptionRegex = Pattern.compile("exception", Pattern.CASE_INSENSITIVE);

    private static final int MAX_LOG_SIZE = 2000;

    private static final Duration IMAGE_PULL_TIMEOUT = Duration.ofMinutes(5);

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...[truncated]";
    }

    @PostConstruct
    public void postConstruct() {
        tempFolderPath = Paths.get(tempFolder).toAbsolutePath().normalize();
        if (!Files.exists(tempFolderPath)) {
            try {
                Files.createDirectories(tempFolderPath);
            } catch (IOException e) {
                log.warn("Failed to create temp folder for plugin execution: {}", tempFolderPath, e);
            }
        }
        log.info("JarExecutor initialized: tempFolder={}, runnerJar={}", tempFolderPath, runnerJar);
        try {
            dockerClient.pingCmd().exec();
            log.info("Docker daemon is reachable via {}", properties.host());
        } catch (Exception e) {
            log.warn("Docker daemon is NOT reachable via {}: external plugins will fail. " +
                    "Check that the docker socket is mounted and the container user can access it", properties.host(), e);
        }
    }

    private void ensureRunnerImagePulled(String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            log.info("Runner image not found locally, pulling: {}", image);
            var callback = new PullImageResultCallback();
            try {
                dockerClient.pullImageCmd(image).exec(callback);
                if (!callback.awaitCompletion(IMAGE_PULL_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                    throw new BusinessException(Status.INTERNAL,
                            "Не удалось скачать образ для запуска плагина за " + IMAGE_PULL_TIMEOUT + ": " + image);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new BusinessException(Status.INTERNAL, "Прервано скачивание образа плагина: " + image);
            }
            log.info("Runner image pulled: {}", image);
        }
    }

    @SneakyThrows
    public String executeJar(PluginEntity plugin, PluginOuterClass.Solution solution) {
        String containerId = null;
        Path pluginPath = null;
        Path runnerPath = null;
        Path graphPath = null;
        try {
            if (solution.getPayloadCase() != PluginOuterClass.Solution.PayloadCase.GRAPH) {
                log.warn("Plugin does not support graphs: pluginId={}, payloadCase={}",
                        plugin.getId(), solution.getPayloadCase());
                throw new BusinessException(Status.INVALID_ARGUMENT, "Плагин не поддерживает графы");
            }
            if (!Files.exists(tempFolderPath)) {
                log.debug("Creating temp folder for plugin execution: {}", tempFolderPath);
                Files.createDirectories(tempFolderPath);
            }
            var graph = graphMapper.toGraph(solution.getGraph());
            var randomId = UUID.randomUUID();
            var pluginClassName = plugin.getJarName();
            var pluginJarFile = plugin.getJarFile();
            log.debug("Executing plugin jar: pluginId={}, pluginClass={}, jarSize={} bytes, graphId={}, vertices={}, edges={}, runId={}",
                    plugin.getId(), pluginClassName, pluginJarFile == null ? 0 : pluginJarFile.length,
                    solution.getGraph().getId(), graph.getVertexCount(), graph.getEdgeCount(), randomId);
            var pluginName = pluginClassName + "-" + randomId + ".jar";
            var jsonGraphFileName = "graph-" + randomId + ".json";
            var runnerFileName = "runner-" + randomId + ".jar";
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
            log.debug("Plugin execution files prepared: plugin={}, runner={}, graph={}, graphJsonSize={} bytes",
                    pluginPath.getFileName(), runnerPath.getFileName(), graphPath.getFileName(), jsonGraph.length());
            ensureRunnerImagePulled(properties.runnerImage());
            var container = dockerClient.createContainerCmd(properties.runnerImage())
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
                    ).exec();
            containerId = container.getId();
            log.info("Plugin container created: containerId={}, pluginId={}, pluginClass={}",
                    containerId, plugin.getId(), pluginClassName);
            for (Path file : List.of(pluginPath, runnerPath, graphPath)) {
                dockerClient.copyArchiveToContainerCmd(containerId)
                        .withHostResource(file.toAbsolutePath().toString())
                        .withRemotePath(containerBaseDir)
                        .exec();
            }
            log.debug("Plugin execution files copied into container: containerId={}", containerId);
            var callback = new WaitContainerResultCallback();
            dockerClient.startContainerCmd(containerId)
                    .exec();
            log.debug("Plugin container started: containerId={}", containerId);
            dockerClient.waitContainerCmd(container.getId())
                    .exec(callback);
            String result;
            var executionStartTime = System.nanoTime();
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
                var durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - executionStartTime);
                log.info("Container exited with: {}", exitCode);
                log.info("Plugin execution finished: pluginId={}, pluginClass={}, exitCode={}, duration={} ms, logLines={}",
                        plugin.getId(), pluginClassName, exitCode, durationMs, logsBuilder.size());

                if (exitCode != 0) {
                    log.warn("Plugin container exited with non-zero code: pluginId={}, exitCode={}, containerId={}, logs={}",
                            plugin.getId(), exitCode, containerId,
                            truncate(String.join("\n", logsBuilder), MAX_LOG_SIZE));
                }

                String logs = String.join("\n", logsBuilder);
                if(exceptionRegex.matcher(logs).find()){
                    log.warn("Plugin execution finished with exception: pluginId={}, logs={}",
                            plugin.getId(), truncate(logs, MAX_LOG_SIZE));
                    throw new PluginExecutionException(logs);
                }
                if (logsBuilder.isEmpty()) {
                    log.warn("Plugin execution produced no output: pluginId={}, containerId={}",
                            plugin.getId(), containerId);
                    throw new BusinessException(Status.INTERNAL, "Плагин не вернул результат выполнения");
                }
                result = logsBuilder.getLast();
                log.debug("Plugin execution result: pluginId={}, result={}", plugin.getId(), result);
            } catch (DockerClientException e) {
                log.warn("Plugin execution timed out after {}: pluginId={}, containerId={}",
                        properties.containerWorkTimeout(), plugin.getId(), containerId, e);
                dockerClient.stopContainerCmd(container.getId()).withTimeout(5).exec();
                throw new BusinessException(Status.DEADLINE_EXCEEDED,
                        "Время выполнения плагина превышино. " +
                                "Время выполнения ограничено до " + properties.containerWorkTimeout());
            }
            return result;
        } catch (BusinessException | PluginExecutionException e) {
            log.warn("Plugin jar execution failed: pluginId={}, message={}",
                    plugin.getId(), truncate(e.getMessage() == null ? "" : e.getMessage(), MAX_LOG_SIZE));
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while executing plugin jar: pluginId={}, pluginClass={}",
                    plugin.getId(), plugin.getJarName(), e);
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
                log.debug("Temporary plugin files removed: plugin={}, runner={}, graph={}",
                        pluginPath, runnerPath, graphPath);
            } catch (Exception e) {
                log.warn("Failed to delete temporary files", e);
            }
        }
    }
}
