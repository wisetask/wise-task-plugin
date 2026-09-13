package ru.leti.wise.task.plugin.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;
import ru.leti.wise.task.graph.GraphServiceGrpc;

@Configuration
@ImportGrpcClients(
        target = "${grpc.service.graph.host}:${grpc.service.graph.port}",
        types = GraphServiceGrpc.GraphServiceBlockingStub.class
)
public class GrpcConfiguration {
}
