package com.pruebatecnica.solicitudes.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.entity.Medicamento;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import org.junit.jupiter.api.Test;

class SolicitudNoPosValidatorTest {

    private final SolicitudNoPosValidator validator = new SolicitudNoPosValidator();
    private final Medicamento pos = new Medicamento("Acetaminofén", true, true);
    private final Medicamento noPos = new Medicamento("Adalimumab", false, true);

    @Test
    void medicamentoPos_noExigeDatosAdicionales() {
        SolicitudRequest sinDatos = new SolicitudRequest(1L, null, null, null, null);

        assertThatCode(() -> validator.validate(pos, sinDatos)).doesNotThrowAnyException();
    }

    @Test
    void medicamentoNoPos_conTodosLosDatos_esValido() {
        SolicitudRequest completo = new SolicitudRequest(2L, "ORD-1", "Calle 1 # 2-3", "3001234567", "a@b.co");

        assertThatCode(() -> validator.validate(noPos, completo)).doesNotThrowAnyException();
    }

    @Test
    void medicamentoNoPos_sinDatos_reportaLosCuatroCamposEnOrden() {
        SolicitudRequest sinDatos = new SolicitudRequest(2L, null, null, null, null);

        assertThatThrownBy(() -> validator.validate(noPos, sinDatos))
                .isInstanceOfSatisfying(RequestValidationException.class, ex ->
                        assertThat(ex.getErrors().keySet())
                                .containsExactly("numeroOrden", "direccion", "telefono", "correoContacto"));
    }

    @Test
    void medicamentoNoPos_conTextosEnBlanco_losTrataComoFaltantes() {
        // el constructor del record normaliza "   " a null
        SolicitudRequest enBlanco = new SolicitudRequest(2L, "   ", "Calle 1", "3001234567", "");

        assertThatThrownBy(() -> validator.validate(noPos, enBlanco))
                .isInstanceOfSatisfying(RequestValidationException.class, ex ->
                        assertThat(ex.getErrors().keySet()).containsExactly("numeroOrden", "correoContacto"));
    }
}
