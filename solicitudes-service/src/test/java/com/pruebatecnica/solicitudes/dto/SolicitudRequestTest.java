package com.pruebatecnica.solicitudes.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SolicitudRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normaliza_textosVaciosANullYLimpiaElTelefono() {
        SolicitudRequest request = new SolicitudRequest(1L, "  ", " Calle 1 ", "(300) 123-4567", "   ");

        assertThat(request.numeroOrden()).isNull();
        assertThat(request.direccion()).isEqualTo("Calle 1");
        assertThat(request.telefono()).isEqualTo("3001234567");
        assertThat(request.correoContacto()).isNull();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void medicamentoEsObligatorio() {
        assertThat(campos(validator.validate(new SolicitudRequest(null, null, null, null, null))))
                .containsExactly("medicamentoId");
    }

    @Test
    void rechazaTelefonoYCorreoConFormatoInvalido() {
        SolicitudRequest request = new SolicitudRequest(1L, "ORD", "Calle", "abc123", "correo-sin-arroba");

        assertThat(campos(validator.validate(request))).containsExactlyInAnyOrder("telefono", "correoContacto");
    }

    private static Set<String> campos(Set<ConstraintViolation<SolicitudRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
