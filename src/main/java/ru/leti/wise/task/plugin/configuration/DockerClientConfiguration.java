package ru.leti.wise.task.plugin.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.leti.wise.task.plugin.configuration.props.DockerProperties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DockerClientConfiguration {
    private final DockerProperties dockerProperties;

    @Bean
    public DockerClientConfig dockerConfig() {
        log.info("Configuring Docker client: host={}, connectionTimeout={}, responseTimeout={}",
                dockerProperties.host(), dockerProperties.connectionTimeout(), dockerProperties.responseTimeout());
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerProperties.host())
                .withDockerTlsVerify(false)
                .build();
    }

    @Bean
    public DockerHttpClient dockerTransport(DockerClientConfig config) {
        log.debug("Configuring Docker transport: maxConnections=100, dockerHost={}", config.getDockerHost());
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(dockerProperties.connectionTimeout())
                .responseTimeout(dockerProperties.responseTimeout())
                .build();
    }

    @Bean
    public DockerClient dockerClient(DockerClientConfig config, DockerHttpClient dockerTransport) {
        var dockerClient = DockerClientImpl.getInstance(config, dockerTransport);
        log.info("Docker client initialized: host={}", config.getDockerHost());
        return dockerClient;
    }
}
