package com.pruebatecnica.solicitudes.notification;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NotificationsProperties.class)
public class NotificationClientConfig {

    @Bean
    RestClient notificationsRestClient(RestClient.Builder builder, NotificationsProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        requestFactory.setReadTimeout(properties.timeout());
        return builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }
}
