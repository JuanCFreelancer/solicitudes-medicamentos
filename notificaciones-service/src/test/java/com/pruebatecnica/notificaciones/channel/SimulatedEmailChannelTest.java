package com.pruebatecnica.notificaciones.channel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimulatedEmailChannelTest {

    @Test
    void maskEmail_ocultaLaParteLocalSinPerderElDominio() {
        assertThat(SimulatedEmailChannel.maskEmail("ana@correo.com")).isEqualTo("a**@correo.com");
        assertThat(SimulatedEmailChannel.maskEmail("juan.perez@empresa.co")).isEqualTo("j*********@empresa.co");
    }

    @Test
    void maskEmail_conParteLocalDeUnCaracter_noExponeNada() {
        assertThat(SimulatedEmailChannel.maskEmail("a@correo.com")).isEqualTo("***@correo.com");
    }

    @Test
    void send_noLanzaExcepcion() {
        new SimulatedEmailChannel().send(new NotificationMessage("ana@correo.com", "Asunto", "Mensaje"));
    }
}
