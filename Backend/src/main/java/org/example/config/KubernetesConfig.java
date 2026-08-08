package org.example.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author pc
 **/
@Configuration
public class KubernetesConfig {
    @Bean
    public KubernetesClient kubernetesClient(
            @Value("${app.kubernetes.connection-timeout-ms:5000}") int connectionTimeout,
            @Value("${app.kubernetes.request-timeout-ms:15000}") int requestTimeout) {
        var config = new ConfigBuilder().withConnectionTimeout(connectionTimeout)
                .withRequestTimeout(requestTimeout).build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
