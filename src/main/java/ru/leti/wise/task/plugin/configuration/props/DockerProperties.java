package ru.leti.wise.task.plugin.configuration.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("spring.docker")
public record DockerProperties(
        String host,
        Duration connectionTimeout,
        Duration responseTimeout,
        Long memoryUsageMb,
        Long cpuQuota,
        Duration containerWorkTimeout
) {
}
