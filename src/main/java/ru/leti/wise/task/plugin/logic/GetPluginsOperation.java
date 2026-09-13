package ru.leti.wise.task.plugin.logic;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginsResponse;
import ru.leti.wise.task.plugin.domain.GraphType;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.PluginType;
import ru.leti.wise.task.plugin.mapper.PluginMapper;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GetPluginsOperation {

    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;

    public GetAllPluginsResponse activate(PluginGrpc.GetAllPluginRequest request) {
        var filter = request.getFilter();
        var paginationRequest = request.getPagination();
        var pageable = PageRequest.of(paginationRequest.getPage(), paginationRequest.getPageSize());
        var specification = byFilter(
                filter
        );

        var pluginPage = pluginRepository.findAll(
                specification,
                pageable
        );
        var plugins = pluginMapper.pluginEntitiesToPlugins(pluginPage.getContent());

        var paginationResponse = PluginGrpc.PaginationResponse.newBuilder()
                .setPage(pluginPage.getNumber())
                .setPageSize(pluginPage.getSize())
                .setTotalCount(pluginPage.getTotalElements())
                .setTotalPages(pluginPage.getTotalPages())
                .setHasNext(pluginPage.hasNext())
                .setHasPrevious(pluginPage.hasPrevious())
                .build();
        return GetAllPluginsResponse.newBuilder()
                .addAllItems(plugins)
                .setPagination(paginationResponse)
                .build();
    }

    public Specification<PluginEntity> byFilter(PluginGrpc.PluginFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.hasName())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            if (filter.hasDescription())
                predicates.add(cb.like(root.get("description"), "%" + filter.getDescription().toLowerCase() + "%"));
            if (filter.hasCategory())
                predicates.add(cb.like(root.get("category"), "%s" + filter.getCategory().toLowerCase() + "%s"));

            if (filter.hasGraphType())
                predicates.add(cb.equal(
                        root.get("graphType"),
                        GraphType.valueOf(filter.getGraphType().name())
                ));

            if (filter.hasIsValid())
                predicates.add(cb.equal(root.get("isValid"), filter.getIsValid()));

            if (filter.hasPluginType())
                predicates.add(cb.equal(
                        root.get("pluginType"),
                        PluginType.valueOf(filter.getPluginType().name())
                ));

            if (filter.hasIsInternal())
                predicates.add(cb.equal(root.get("isInternal"), filter.getIsInternal()));

            if (filter.hasAuthorId())
                predicates.add(cb.equal(root.get("authorId"), filter.getAuthorId()));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
