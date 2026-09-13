package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.ErrorCode;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ValidatePluginOperation {

    private final PluginRepository pluginRepository;

    public PluginGrpc.ValidatePluginResponse activate(UUID id) {
        var plugin = pluginRepository.findById(id);
        pluginRepository.save(plugin.map(pluginEntity -> {
                    pluginEntity.setIsValid(true);
                    return pluginEntity;
                }).orElseThrow(() -> new BusinessException(Status.NOT_FOUND,
                                "Плагин с id: %s не найден".formatted(id)
                        )
                )
        );
        return PluginGrpc.ValidatePluginResponse.newBuilder().setId(id.toString()).build();
    }
}
