package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.CheckPluginImplementationRequest;
import ru.leti.wise.task.plugin.PluginGrpc.CheckPluginImplementationResponse;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.PluginOuterClass.GraphTestResult;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.PluginType;
import ru.leti.wise.task.plugin.domain.graph.external.ExternalPluginService;
import ru.leti.wise.task.plugin.domain.graph.internal.InternalPluginService;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.repository.PluginRepository;
import ru.leti.wise.task.plugin.service.PluginValidationService;
import ru.leti.wise.task.plugin.service.grpc.GraphGrpcService;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.UUID.fromString;

@Component
@RequiredArgsConstructor
public class CheckPluginImplementationOperation {

    private final PluginRepository pluginRepository;
    private final GraphGrpcService graphGrpcService;
    private final ExternalPluginService externalPluginService;
    private final InternalPluginService internalPluginService;

    public CheckPluginImplementationResponse activate(CheckPluginImplementationRequest request) {

        var basePluginEntity = pluginRepository.findById(fromString(request.getId()))
                .orElseThrow(() -> new BusinessException(Status.NOT_FOUND, "Плагин не найден"));

        if (basePluginEntity.getPluginType() != PluginType.GRAPH_PROPERTY
                && basePluginEntity.getPluginType() != PluginType.GRAPH_CHARACTERISTIC) {
            throw new BusinessException(Status.INVALID_ARGUMENT, "Тип плагина некорректный");
        }

        List<GraphTestResult> graphTestResults = new ArrayList<>();
        boolean result = true;
        var newPluginEntity = PluginEntity.builder()
                .pluginType(basePluginEntity.getPluginType())
                .jarFile(Base64.getDecoder().decode(request.getFile()))
                .jarName(basePluginEntity.getJarName())
                .build();

        for (int i = 0; i < 5; i++) {
            var graphTestResult = runAlgorithms(basePluginEntity, newPluginEntity);
            if (!graphTestResult.getResult().equals(graphTestResult.getOriginalResult())) {
                result = false;
            }
            graphTestResults.add(graphTestResult);

        }

        return CheckPluginImplementationResponse.newBuilder()
                .setImplementationResult(PluginOuterClass.ImplementationResult.newBuilder()
                        .setResult(result)
                        .addAllGraphTestResults(graphTestResults)
                        .build())
                .build();
    }

    private GraphTestResult runAlgorithms(PluginEntity basePluginEntity, PluginEntity newPluginEntity) {
        var solution = generateSolution();
        final long baseStartTime = System.nanoTime();
        var originalResult = basePluginEntity.getIsInternal()
                ? internalPluginService.run(basePluginEntity, solution)
                : externalPluginService.run(basePluginEntity, solution);
        final long originalDuration = System.nanoTime() - baseStartTime;

        final long newStartTime = System.nanoTime();
        var newResult = externalPluginService.run(newPluginEntity, solution);
        final long newDuration = System.nanoTime() - newStartTime;

        return GraphTestResult.newBuilder()
                .setGraphId(solution.getGraph().getId())
                .setOriginalTimeResult(originalDuration)
                .setTimeResult(newDuration)
                .setResult(newResult)
                .setOriginalResult(originalResult)
                .build();
    }


    private Solution generateSolution() {
        var vertexCount = ThreadLocalRandom.current().nextInt(1, 6);
        var edgeCount = ThreadLocalRandom.current().nextInt(1, 6);
        var isDirect = ThreadLocalRandom.current().nextBoolean();
        var graph = graphGrpcService.getGraph(vertexCount, edgeCount, isDirect);
        return PluginOuterClass.Solution.newBuilder()
                .setGraph(graph)
                .build();
    }
}
