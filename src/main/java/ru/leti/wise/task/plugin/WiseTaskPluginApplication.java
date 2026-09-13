package ru.leti.wise.task.plugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.leti.wise.task.plugin.configuration.props.DockerProperties;

@SpringBootApplication
@EnableConfigurationProperties(DockerProperties.class)
public class WiseTaskPluginApplication {
    static void main(String[] args) {
        SpringApplication.run(WiseTaskPluginApplication.class, args);
    }
}
