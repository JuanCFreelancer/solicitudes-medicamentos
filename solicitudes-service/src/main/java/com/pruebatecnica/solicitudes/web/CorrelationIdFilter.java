package com.pruebatecnica.solicitudes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traza un proceso de negocio de extremo a extremo a través de varios servicios.
 * <p>
 * Toma el identificador de la cabecera {@value #HEADER} (o genera uno), lo deja disponible en el log
 * (MDC {@value #MDC_KEY}) y lo devuelve en la respuesta. Cuando un servicio llama a otro reenvía la
 * misma cabecera, por lo que buscar un id en los logs reconstruye el recorrido completo.
 * Se ejecuta antes que Spring Security para que también las respuestas 401/403 lleven el id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /** Solo se acepta un valor "seguro": evita inyección de saltos de línea o texto arbitrario en los logs. */
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String correlationId = incoming != null && VALID_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
