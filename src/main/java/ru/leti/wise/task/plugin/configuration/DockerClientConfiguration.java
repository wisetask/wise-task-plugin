package ru.leti.wise.task.plugin.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.leti.wise.task.plugin.configuration.props.DockerProperties;

@Configuration
@RequiredArgsConstructor
public class DockerClientConfiguration {
    private final DockerProperties dockerProperties;

    @Bean
    public DockerClientConfig dockerConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerProperties.host())
                .withDockerTlsVerify(false)
                .build();
    }

    @Bean
    public DockerHttpClient dockerTransport(DockerClientConfig config) {
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
        return DockerClientImpl.getInstance(config, dockerTransport);
    }
}
