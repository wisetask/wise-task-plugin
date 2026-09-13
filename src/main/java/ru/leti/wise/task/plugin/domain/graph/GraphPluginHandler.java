package ru.leti.wise.task.plugin.domain.graph;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class GraphPluginHandler {

    private final GraphMapper graphMapper;

    public String run(Plugin plugin, Solution solution) {
        if (solution.getPayloadCase() != Solution.PayloadCase.GRAPH) {
            throw new BusinessException(Status.INVALID_ARGUMENT, "Плагин поддерживает только графы");
        }
        var grpcGraph = solution.getGraph();

        var graph = graphMapper.toGraph(grpcGraph);
        try {
            return switch (plugin) {
                case GraphProperty p -> valueOf(p.run(graph));
                case GraphCharacteristic p -> valueOf(p.run(graph));
                case HandwrittenAnswer p -> valueOf(p.run(graph, solution.getHandwrittenAnswer()));
                case NewGraphConstruction p -> valueOf(p.run(graph, graphMapper.toGraph(solution.getOtherGraph())));
                case null, default -> throw new IllegalStateException("Unexpected value: " + plugin);
            };
        } catch (Exception e) {
            throw new PluginExecutionException(e.getMessage());
        }
    }
}
