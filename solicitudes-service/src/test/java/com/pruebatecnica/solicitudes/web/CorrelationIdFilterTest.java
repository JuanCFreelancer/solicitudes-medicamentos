package com.pruebatecnica.solicitudes.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    /** Ejecuta el filtro y devuelve el valor que vio el "resto de la aplicación" en el MDC. */
    private String run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
        });
        return seen.get();
    }

    @Test
    void sinCabecera_generaUnIdYLoDevuelveEnLaRespuesta() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = run(new MockHttpServletRequest(), response);

        assertThat(seen).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(seen);
    }

    @Test
    void conCabeceraValida_laReutilizaParaUnirLosLogsDeVariosServicios() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123.XYZ_9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = run(request, response);

        assertThat(seen).isEqualTo("abc-123.XYZ_9");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123.XYZ_9");
    }

    @Test
    void conCabeceraPeligrosa_laDescartaYGeneraOtra() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "x\r\nINFO forged log line");

        String seen = run(request, new MockHttpServletResponse());

        assertThat(seen).doesNotContain("forged").doesNotContain("\n").hasSize(36); // UUID
    }

    @Test
    void alTerminar_limpiaElMdcParaNoFiltrarElIdAOtraPeticionDelMismoHilo() throws Exception {
        run(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
