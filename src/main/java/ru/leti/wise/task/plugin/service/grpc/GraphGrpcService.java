package ru.leti.wise.task.plugin.service.grpc;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.GraphGrpc.*;
import ru.leti.wise.task.graph.GraphOuterClass.*;
import ru.leti.wise.task.graph.GraphServiceGrpc;


@Component
@Observed
@RequiredArgsConstructor
public class GraphGrpcService {

    private final GraphServiceGrpc.GraphServiceBlockingStub graphService;

    public Graph getGraph(int edgeCount, int vertexCount, boolean isDirect) {
        var request = GenerateGraphRequest.newBuilder()
                .setEdgeCount(edgeCount)
                .setVertexCount(vertexCount)
                .setIsDirect(isDirect)
                .build();

        return graphService.generateRandomGraph(request).getGraph();
    }
}
