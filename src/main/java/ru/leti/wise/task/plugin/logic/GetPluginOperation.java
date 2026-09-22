package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.GetPluginResponse;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.ErrorCode;
import ru.leti.wise.task.plugin.mapper.PluginMapper;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPluginOperation {

    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;

    public GetPluginResponse activate(UUID id) {
        log.debug("Fetching plugin: id={}", id);
        var plugin = pluginRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Plugin not found: id={}", id);
                    return new BusinessException(Status.NOT_FOUND,
                            "Плагин с id: %s не найден".formatted(id));
                });
        log.debug("Plugin found: id={}, name={}, pluginType={}, internal={}",
                plugin.getId(), plugin.getName(), plugin.getPluginType(), plugin.getIsInternal());
        return GetPluginResponse.newBuilder()
                .setPlugin(pluginMapper.pluginEntityToPlugin(plugin))
                .build();
    }
}
