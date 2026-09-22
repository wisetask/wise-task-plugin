package ru.leti.wise.task.plugin.domain.graph;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.Plugin;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.PluginExecutionException;
import ru.leti.wise.task.plugin.graph.GraphCharacteristic;
import ru.leti.wise.task.plugin.graph.GraphProperty;
import ru.leti.wise.task.plugin.graph.HandwrittenAnswer;
import ru.leti.wise.task.plugin.graph.NewGraphConstruction;
import ru.leti.wise.task.plugin.mapper.GraphMapper;

import static java.lang.String.valueOf;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphPluginHandler {

    private final GraphMapper graphMapper;

    public String run(Plugin plugin, Solution solution) {
        var pluginName = plugin == null ? "null" : plugin.getClass().getSimpleName();
        if (solution.getPayloadCase() != Solution.PayloadCase.GRAPH) {
            log.warn("Plugin supports only graphs: plugin={}, payloadCase={}", pluginName, solution.getPayloadCase());
            throw new BusinessException(Status.INVALID_ARGUMENT, "Плагин поддерживает только графы");
        }
        var grpcGraph = solution.getGraph();
        log.debug("Running graph plugin: plugin={}, graphId={}, vertices={}, edges={}",
                pluginName, grpcGraph.getId(), grpcGraph.getVertexListCount(), grpcGraph.getEdgeListCount());

        var graph = graphMapper.toGraph(grpcGraph);
        try {
            var result = switch (plugin) {
                case GraphProperty p -> valueOf(p.run(graph));
                case GraphCharacteristic p -> valueOf(p.run(graph));
                case HandwrittenAnswer p -> valueOf(p.run(graph, solution.getHandwrittenAnswer()));
                case NewGraphConstruction p -> valueOf(p.run(graph, graphMapper.toGraph(solution.getOtherGraph())));
                case null, default -> throw new IllegalStateException("Unexpected value: " + plugin);
            };
            log.debug("Graph plugin finished: plugin={}, graphId={}, result={}", pluginName, grpcGraph.getId(), result);
            return result;
        } catch (Exception e) {
            log.warn("Graph plugin failed: plugin={}, graphId={}, reason={}",
                    pluginName, grpcGraph.getId(), e.getMessage());
            throw new PluginExecutionException(e.getMessage());
        }
    }
}
