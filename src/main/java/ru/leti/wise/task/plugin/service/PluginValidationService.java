package ru.leti.wise.task.plugin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.graph.external.ExternalPluginService;
import ru.leti.wise.task.plugin.service.grpc.GraphGrpcService;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
// TODO другие типы плагинов
public class PluginValidationService {
    private final GraphGrpcService graphGrpcService;
    private final ExternalPluginService externalPluginService;

    public void validatePlugin(PluginEntity pluginEntity) {
        testAbstractPlugin(pluginEntity);
    }

    private void testAbstractPlugin(PluginEntity pluginEntity) {
        externalPluginService.run(pluginEntity, prepareSolution());
    }

    private Solution prepareSolution() {
        var graph = getGraph();
        var solutionBuilder = Solution.newBuilder();
        return solutionBuilder.setGraph(graph).build();
    }

    private GraphOuterClass.Graph getGraph() {
        var vertexCount = ThreadLocalRandom.current().nextInt(1, 6);
        var edgeCount = ThreadLocalRandom.current().nextInt(1, 6);
        var isDirect = ThreadLocalRandom.current().nextBoolean();
        return graphGrpcService.getGraph(vertexCount, edgeCount, isDirect);
    }
}
