package ru.leti.wise.task.plugin.logic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeletePluginOperation {

    private final PluginRepository pluginRepository;

    public void activate(UUID id) {
        log.info("Deleting plugin: id={}", id);
        if (!pluginRepository.existsById(id)) {
            log.warn("Plugin not found for deletion: id={}", id);
        }
        pluginRepository.deleteById(id);
        log.info("Plugin deleted: id={}", id);
    }

}
