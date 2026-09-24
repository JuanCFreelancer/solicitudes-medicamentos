package com.pruebatecnica.solicitudes.service;

import com.pruebatecnica.solicitudes.config.PaginationProperties;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Construye la paginación validando los límites configurados. El orden es fijo (más recientes
 * primero): no se acepta {@code sort} del cliente para no exponer nombres internos de columnas.
 */
@Component
@EnableConfigurationProperties(PaginationProperties.class)
public class PageRequestFactory {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PaginationProperties properties;

    public PageRequestFactory(PaginationProperties properties) {
        this.properties = properties;
    }

    public PageRequest create(Integer page, Integer size) {
        int effectivePage = page == null ? 0 : page;
        int effectiveSize = size == null ? properties.defaultSize() : size;

        Map<String, String> errors = new LinkedHashMap<>();
        if (effectivePage < 0) {
            errors.put("page", "La página debe ser mayor o igual a 0");
        }
        if (effectiveSize < 1 || effectiveSize > properties.maxSize()) {
            errors.put("size", "El tamaño de página debe estar entre 1 y " + properties.maxSize());
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return PageRequest.of(effectivePage, effectiveSize, NEWEST_FIRST);
    }
}
