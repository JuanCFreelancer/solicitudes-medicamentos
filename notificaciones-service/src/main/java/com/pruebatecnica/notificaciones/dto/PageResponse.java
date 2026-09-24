package com.pruebatecnica.notificaciones.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Contrato JSON estable para listados paginados (no se serializa el {@code PageImpl} de Spring,
 * cuyo formato no está garantizado entre versiones).
 * {@code page} es base 0.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
