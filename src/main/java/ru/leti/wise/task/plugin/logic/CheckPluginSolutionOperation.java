package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.CheckPluginSolutionRequest;
import ru.leti.wise.task.plugin.PluginGrpc.CheckPluginSolutionResponse;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.graph.external.ExternalPluginService;
import ru.leti.wise.task.plugin.domain.graph.internal.InternalPluginService;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.ErrorCode;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckPluginSolutionOperation {
    private final PluginRepository pluginRepository;
    private final InternalPluginService internalPluginService;
    private final ExternalPluginService externalPluginService;

    public CheckPluginSolutionResponse activate(CheckPluginSolutionRequest request) {

        var solution = request.getSolution();
        var pluginId = UUID.fromString(solution.getPluginId());
        log.debug("Checking plugin solution: pluginId={}, payloadCase={}", pluginId, solution.getPayloadCase());
        PluginEntity pluginEntity = pluginRepository.findById(pluginId)
                .orElseThrow(() -> {
                    log.warn("Plugin not found for solution check: id={}", pluginId);
                    return new BusinessException(Status.NOT_FOUND,
                            "Плагин с id: %s не найден".formatted(pluginId));
                });

        var startTime = System.nanoTime();
        var response = pluginEntity.getIsInternal()
                ? buildInternalPluginResponse(pluginEntity, solution)
                : buildExternalPluginResponse(pluginEntity, solution);
        log.info("Plugin solution checked: pluginId={}, internal={}, result={}, duration={} ms",
                pluginId, pluginEntity.getIsInternal(), response.getResult(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
        return response;
    }

    private CheckPluginSolutionResponse buildInternalPluginResponse(PluginEntity pluginEntity, Solution solution) {
        log.debug("Running internal plugin solution check: pluginId={}, beanName={}",
                pluginEntity.getId(), pluginEntity.getBeanName());
        var result = internalPluginService.run(pluginEntity, solution);
        return CheckPluginSolutionResponse.newBuilder()
                .setResult(result)
                .build();
    }

    private CheckPluginSolutionResponse buildExternalPluginResponse(PluginEntity pluginEntity, Solution solution) {
        log.debug("Running external plugin solution check: pluginId={}, pluginClass={}",
                pluginEntity.getId(), pluginEntity.getJarName());
        var result = externalPluginService.run(pluginEntity, solution);
        return CheckPluginSolutionResponse.newBuilder()
                .setResult(result)
                .build();
    }
}
