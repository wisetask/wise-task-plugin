package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeUnit;

import static java.util.UUID.fromString;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckPluginImplementationOperation {

    private static final int TEST_RUNS = 5;

    private final PluginRepository pluginRepository;
    private final GraphGrpcService graphGrpcService;
    private final ExternalPluginService externalPluginService;
    private final InternalPluginService internalPluginService;

    public CheckPluginImplementationResponse activate(CheckPluginImplementationRequest request) {

        var basePluginEntity = pluginRepository.findById(fromString(request.getId()))
                .orElseThrow(() -> {
                    log.warn("Plugin not found for implementation check: id={}", request.getId());
                    return new BusinessException(Status.NOT_FOUND, "Плагин не найден");
                });

        if (basePluginEntity.getPluginType() != PluginType.GRAPH_PROPERTY
                && basePluginEntity.getPluginType() != PluginType.GRAPH_CHARACTERISTIC) {
            log.warn("Unsupported plugin type for implementation check: pluginId={}, pluginType={}",
                    basePluginEntity.getId(), basePluginEntity.getPluginType());
            throw new BusinessException(Status.INVALID_ARGUMENT, "Тип плагина некорректный");
        }

        log.info("Checking plugin implementation: basePluginId={}, pluginType={}, jarSize={} bytes, runs={}",
                basePluginEntity.getId(), basePluginEntity.getPluginType(),
                request.getFile().length(), TEST_RUNS);

        List<GraphTestResult> graphTestResults = new ArrayList<>();
        boolean result = true;
        var newPluginEntity = PluginEntity.builder()
                .pluginType(basePluginEntity.getPluginType())
                .jarFile(Base64.getDecoder().decode(request.getFile()))
                .jarName(basePluginEntity.getJarName())
                .build();

        for (int i = 0; i < TEST_RUNS; i++) {
            var graphTestResult = runAlgorithms(basePluginEntity, newPluginEntity);
            if (!graphTestResult.getResult().equals(graphTestResult.getOriginalResult())) {
                result = false;
                log.debug("Implementation test run #{}: mismatch for graphId={}, original={}, new={}",
                        i + 1, graphTestResult.getGraphId(),
                        graphTestResult.getOriginalResult(), graphTestResult.getResult());
            } else {
                log.debug("Implementation test run #{}: match for graphId={}, result={}",
                        i + 1, graphTestResult.getGraphId(), graphTestResult.getResult());
            }
            graphTestResults.add(graphTestResult);

        }

        log.info("Plugin implementation checked: basePluginId={}, result={}, runs={}",
                basePluginEntity.getId(), result, graphTestResults.size());

        return CheckPluginImplementationResponse.newBuilder()
                .setImplementationResult(PluginOuterClass.ImplementationResult.newBuilder()
                        .setResult(result)
                        .addAllGraphTestResults(graphTestResults)
                        .build())
                .build();
    }

    private GraphTestResult runAlgorithms(PluginEntity basePluginEntity, PluginEntity newPluginEntity) {
        var solution = generateSolution();
        log.debug("Running implementation test: basePluginId={}, newPluginId={}, graphId={}, vertices={}, edges={}",
                basePluginEntity.getId(), newPluginEntity.getId(), solution.getGraph().getId(),
                solution.getGraph().getVertexListCount(), solution.getGraph().getEdgeListCount());
        final long baseStartTime = System.nanoTime();
        var originalResult = basePluginEntity.getIsInternal()
                ? internalPluginService.run(basePluginEntity, solution)
                : externalPluginService.run(basePluginEntity, solution);
        final long originalDuration = System.nanoTime() - baseStartTime;

        final long newStartTime = System.nanoTime();
        var newResult = externalPluginService.run(newPluginEntity, solution);
        final long newDuration = System.nanoTime() - newStartTime;
        log.debug("Implementation test finished: graphId={}, originalResult={}, originalDuration={} ms, newResult={}, newDuration={} ms",
                solution.getGraph().getId(), originalResult, TimeUnit.NANOSECONDS.toMillis(originalDuration),
                newResult, TimeUnit.NANOSECONDS.toMillis(newDuration));

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
