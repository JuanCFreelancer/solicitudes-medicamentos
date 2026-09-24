package com.pruebatecnica.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Orígenes permitidos por CORS (prefijo {@code app.cors}). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
