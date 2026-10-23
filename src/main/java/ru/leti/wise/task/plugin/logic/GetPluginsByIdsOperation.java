package ru.leti.wise.task.plugin.logic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.GetPluginsByIdsResponse;
import ru.leti.wise.task.plugin.PluginGrpc.PluginIds;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.mapper.PluginMapper;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPluginsByIdsOperation {

    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;

    public GetPluginsByIdsResponse activate(PluginIds request) {
        var ids = request.getPluginIdsList()
                .stream()
                .filter(id -> !id.isBlank())
                .map(UUID::fromString)
                .toList();
        log.debug("Fetching plugins by ids: requested={}, uniqueIds={}", request.getPluginIdsList().size(), ids.size());
        var plugins = ids.isEmpty() ? List.<PluginEntity>of() : pluginRepository.findAllById(ids);
        log.debug("Plugins fetched by ids: requested={}, found={}", ids.size(), plugins.size());
        return GetPluginsByIdsResponse.newBuilder()
                .addAllPlugins(pluginMapper.pluginEntitiesToPlugins(plugins))
                .build();
    }
}
