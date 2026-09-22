package ru.leti.wise.task.plugin.domain.graph.external;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.Plugin;
import ru.leti.wise.task.plugin.PluginOuterClass.Solution;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.graph.GraphPluginHandler;
import ru.leti.wise.task.plugin.graph.GraphPlugin;
import ru.leti.wise.task.plugin.helper.JarExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static java.time.LocalTime.now;
import static java.util.UUID.randomUUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalPluginService {

    private final JarExecutor jarExecutor;

    public String run(PluginEntity plugin, Solution solution) {
        log.debug("Running external plugin: pluginId={}, pluginClass={}, graphId={}",
                plugin.getId(), plugin.getJarName(), solution.getGraph().getId());
        var startTime = System.nanoTime();
        try {
            var result = jarExecutor.executeJar(plugin, solution);
            log.debug("External plugin finished: pluginId={}, result={}, duration={} ms",
                    plugin.getId(), result, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            return result;
        } catch (RuntimeException e) {
            log.warn("External plugin failed: pluginId={}, pluginClass={}, duration={} ms, reason={}",
                    plugin.getId(), plugin.getJarName(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime), e.getMessage());
            throw e;
        }
    }
}
