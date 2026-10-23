package ru.leti.wise.task.plugin.service.grpc;

import io.grpc.StatusRuntimeException;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.GraphGrpc.*;
import ru.leti.wise.task.graph.GraphOuterClass.*;
import ru.leti.wise.task.graph.GraphServiceGrpc;


@Slf4j
@Component
@Observed
@RequiredArgsConstructor
public class GraphGrpcService {

    private final GraphServiceGrpc.GraphServiceBlockingStub graphService;

    public Graph getGraph(int edgeCount, int vertexCount, boolean isDirect) {
        log.debug("Requesting random graph: vertexCount={}, edgeCount={}, isDirect={}",
                vertexCount, edgeCount, isDirect);
        var request = GenerateGraphRequest.newBuilder()
                .setEdgeCount(edgeCount)
                .setVertexCount(vertexCount)
                .setIsDirect(isDirect)
                .build();

        try {
            var graph = graphService.generateRandomGraph(request).getGraph();
            log.debug("Random graph received: id={}, vertices={}, edges={}, isDirect={}",
                    graph.getId(), graph.getVertexListCount(), graph.getEdgeListCount(), graph.getIsDirect());
            return graph;
        } catch (StatusRuntimeException e) {
            log.warn("Graph service call generateRandomGraph failed: status={}, vertexCount={}, edgeCount={}",
                    e.getStatus(), vertexCount, edgeCount, e);
            throw e;
        }
    }
}
