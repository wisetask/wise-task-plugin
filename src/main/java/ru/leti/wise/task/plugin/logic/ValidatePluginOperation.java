package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.ErrorCode;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidatePluginOperation {

    private final PluginRepository pluginRepository;

    public PluginGrpc.ValidatePluginResponse activate(UUID id) {
        log.info("Validating (marking as valid) plugin: id={}", id);
        var plugin = pluginRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Plugin not found for validation: id={}", id);
                    return new BusinessException(Status.NOT_FOUND,
                            "Плагин с id: %s не найден".formatted(id));
                });
        plugin.setIsValid(true);
        pluginRepository.save(plugin);
        log.info("Plugin marked as valid: id={}", id);
        return PluginGrpc.ValidatePluginResponse.newBuilder().setId(id.toString()).build();
    }
}
