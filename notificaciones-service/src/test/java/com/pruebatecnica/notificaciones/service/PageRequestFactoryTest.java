package com.pruebatecnica.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pruebatecnica.notificaciones.config.PaginationProperties;
import com.pruebatecnica.notificaciones.exception.RequestValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class PageRequestFactoryTest {

    private final PageRequestFactory factory = new PageRequestFactory(new PaginationProperties(10, 50));

    @Test
    void sinParametros_usaValoresPorDefectoYOrdenaPorMasRecientes() {
        PageRequest pageRequest = factory.create(null, null);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(10);
        assertThat(pageRequest.getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
    }

    @Test
    void conParametrosValidos_losRespeta() {
        PageRequest pageRequest = factory.create(3, 50);

        assertThat(pageRequest.getPageNumber()).isEqualTo(3);
        assertThat(pageRequest.getPageSize()).isEqualTo(50);
    }

    @Test
    void rechazaPaginaNegativaYTamanoFueraDeRango() {
        assertThatThrownBy(() -> factory.create(-1, 51))
                .isInstanceOfSatisfying(RequestValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsKeys("page", "size"));
        assertThatThrownBy(() -> factory.create(0, 0)).isInstanceOf(RequestValidationException.class);
    }
}
