package ru.leti.wise.task.plugin.domain.graph.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.leti.wise.task.plugin.Plugin;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.graph.GraphPluginHandler;
import ru.leti.wise.task.plugin.graph.GraphPlugin;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalPluginService {

    private final Map<String, Plugin> plugins;
    private final GraphPluginHandler graphPluginHandler;

    public String run(PluginEntity plugin, Solution solution) {
        var pluginName = plugin.getBeanName();
        log.debug("Running internal plugin: pluginId={}, beanName={}", plugin.getId(), pluginName);
        if (plugins.get(pluginName) instanceof GraphPlugin p) {
            var result = graphPluginHandler.run(p, solution);
            log.debug("Internal plugin finished: pluginId={}, beanName={}, result={}",
                    plugin.getId(), pluginName, result);
            return result;
        } else {
            log.warn("Internal plugin bean not found: pluginId={}, beanName={}, availableBeans={}",
                    plugin.getId(), pluginName, plugins.keySet());
            throw new IllegalStateException("Unexpected value: " + plugins.get(pluginName));
        }
    }
}
