package com.pruebatecnica.notificaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pruebatecnica.notificaciones.config.ProblemDetailSecurityHandlers;
import com.pruebatecnica.notificaciones.config.SecurityConfig;
import com.pruebatecnica.notificaciones.dto.NotificacionResponse;
import com.pruebatecnica.notificaciones.dto.PageResponse;
import com.pruebatecnica.notificaciones.entity.CanalNotificacion;
import com.pruebatecnica.notificaciones.entity.EstadoEnvio;
import com.pruebatecnica.notificaciones.exception.GlobalExceptionHandler;
import com.pruebatecnica.notificaciones.service.NotificacionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(NotificacionController.class)
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class NotificacionControllerTest {

    private static final String BODY = """
            {"destinatario":"ana@correo.com","asunto":"Solicitud #12","mensaje":"Recibimos tu solicitud","referencia":"solicitud:12"}""";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private NotificacionService notificacionService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor usuario(long id) {
        return jwt().jwt(token -> token.subject(String.valueOf(id)));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/notificaciones").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
        mockMvc.perform(get("/notificaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void enviar_usaElIdDelTokenYDevuelve201() throws Exception {
        when(notificacionService.enviar(eq(42L), any())).thenReturn(new NotificacionResponse(
                5L, "ana@correo.com", "Solicitud #12", "Recibimos tu solicitud", CanalNotificacion.EMAIL,
                EstadoEnvio.ENVIADA, "solicitud:12", Instant.parse("2026-01-01T10:00:00Z")));

        mockMvc.perform(post("/notificaciones").with(usuario(42)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.referencia").value("solicitud:12"));

        verify(notificacionService).enviar(eq(42L), any());
    }

    @Test
    void enviar_conDatosInvalidos_devuelve400PorCampo() throws Exception {
        mockMvc.perform(post("/notificaciones").with(usuario(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinatario":"no-es-correo","asunto":"","mensaje":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.destinatario").exists())
                .andExpect(jsonPath("$.errors.asunto").exists())
                .andExpect(jsonPath("$.errors.mensaje").exists());
    }

    @Test
    void listar_pasaPaginacionYUsuarioDelToken() throws Exception {
        when(notificacionService.listar(42L, 1, 5)).thenReturn(new PageResponse<>(List.of(), 1, 5, 6, 2));

        mockMvc.perform(get("/notificaciones").param("page", "1").param("size", "5").with(usuario(42)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6));
    }
}
