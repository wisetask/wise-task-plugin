package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class CheckPluginSolutionOperation {
    private final PluginRepository pluginRepository;
    private final InternalPluginService internalPluginService;
    private final ExternalPluginService externalPluginService;

    public CheckPluginSolutionResponse activate(CheckPluginSolutionRequest request) {

        var solution = request.getSolution();
        var pluginId = UUID.fromString(solution.getPluginId());
        PluginEntity pluginEntity = pluginRepository.findById(pluginId)
                .orElseThrow(() -> new BusinessException(Status.NOT_FOUND,
                                "Плагин с id: %s не найден".formatted(pluginId)
                        )
                );

        return pluginEntity.getIsInternal()
                ? buildInternalPluginResponse(pluginEntity, solution)
                : buildExternalPluginResponse(pluginEntity, solution);
    }

    private CheckPluginSolutionResponse buildInternalPluginResponse(PluginEntity pluginEntity, Solution solution) {
        var result = internalPluginService.run(pluginEntity, solution);
        return CheckPluginSolutionResponse.newBuilder()
                .setResult(result)
                .build();
    }

    private CheckPluginSolutionResponse buildExternalPluginResponse(PluginEntity pluginEntity, Solution solution) {
        var result = externalPluginService.run(pluginEntity, solution);
        return CheckPluginSolutionResponse.newBuilder()
                .setResult(result)
                .build();
    }
}
