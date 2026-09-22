package ru.leti.wise.task.plugin.logic;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.UpdatePluginRequest;
import ru.leti.wise.task.plugin.PluginGrpc.UpdatePluginResponse;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.ErrorCode;
import ru.leti.wise.task.plugin.mapper.PluginMapper;
import ru.leti.wise.task.plugin.repository.PluginRepository;
import ru.leti.wise.task.plugin.service.PluginValidationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdatePluginOperation {

    private final PluginMapper pluginMapper;
    private final PluginRepository pluginRepository;
    private final PluginValidationService pluginValidationService;

    public UpdatePluginResponse activate(UpdatePluginRequest request) {
        var requestPlugin = pluginMapper.pluginToPluginEntity(request.getPlugin());
        log.info("Updating plugin: id={}, name={}", requestPlugin.getId(), requestPlugin.getName());
        var pluginEntity = pluginRepository.findById(requestPlugin.getId())
                .orElseThrow(() -> {
                    log.warn("Plugin not found for update: id={}", requestPlugin.getId());
                    return new BusinessException(Status.NOT_FOUND,
                            "Плагин с id: %s не найден".formatted(requestPlugin.getId()));
                });
        pluginMapper.updatePlugin(requestPlugin, pluginEntity);
        if (requestPlugin.getJarFile() != null) {
            pluginEntity.setJarFile(requestPlugin.getJarFile());
            pluginEntity.setJarName(requestPlugin.getJarName());
            log.debug("Plugin jar updated: id={}, jarName={}, jarSize={} bytes",
                    pluginEntity.getId(), pluginEntity.getJarName(), pluginEntity.getJarFile().length);
            pluginValidationService.validatePlugin(requestPlugin);
            pluginRepository.save(pluginEntity);
            log.info("Plugin updated: id={}", pluginEntity.getId());
        } else {
            log.warn("Cannot update plugin without jar file: id={}", requestPlugin.getId());
            throw new BusinessException(Status.INVALID_ARGUMENT, "Отсутствует Jar файл для плагина");
        }

        return UpdatePluginResponse.newBuilder()
                .setPlugin(pluginMapper.pluginEntityToPlugin(pluginEntity))
                .build();
    }
}
