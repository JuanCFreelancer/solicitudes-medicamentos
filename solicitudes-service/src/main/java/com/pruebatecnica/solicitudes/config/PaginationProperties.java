package com.pruebatecnica.solicitudes.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Límites de paginación (prefijo {@code app.pagination}); evita que un cliente pida páginas gigantes. */
@Validated
@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(@Positive int defaultSize, @Positive int maxSize) {
}
