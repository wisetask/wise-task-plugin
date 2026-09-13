package ru.leti.wise.task.plugin.mapper;


import org.mapstruct.*;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.graph.model.Color;
import ru.leti.wise.task.graph.model.Edge;
import ru.leti.wise.task.graph.model.Graph;
import ru.leti.wise.task.graph.model.Vertex;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface GraphMapper {

    @Mapping(target = "vertexList", ignore = true)
    @Mapping(target = "edgeList", ignore = true)
    Graph toGraph(GraphOuterClass.Graph graph);

    @Mapping(target = "xCoordinate", source = "XCoordinate")
    @Mapping(target = "yCoordinate", source = "YCoordinate")
    Vertex toVertex(GraphOuterClass.Vertex vertex);
    Edge toEdge(GraphOuterClass.Edge edge);

    @ValueMapping(target = "GRAY", source = "UNRECOGNIZED")
    @ValueMapping(target = "GRAY", source = "GRAY")
    Color toColor(GraphOuterClass.Color color);

    @AfterMapping
    default void mapVertexAndEdgeLists(@MappingTarget Graph.GraphBuilder builder, GraphOuterClass.Graph source) {
        if (!source.getVertexListList().isEmpty()) {
            List<Vertex> vertices = source.getVertexListList().stream()
                    .map(this::toVertex)
                    .collect(Collectors.toList());
            builder.vertexList(vertices);
        }

        if (!source.getEdgeListList().isEmpty()) {
            List<Edge> edges = source.getEdgeListList().stream()
                    .map(this::toEdge)
                    .collect(Collectors.toList());
            builder.edgeList(edges);
        }
    }
}
