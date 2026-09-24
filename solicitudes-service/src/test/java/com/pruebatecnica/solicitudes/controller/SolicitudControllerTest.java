package com.pruebatecnica.solicitudes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pruebatecnica.solicitudes.config.ProblemDetailSecurityHandlers;
import com.pruebatecnica.solicitudes.config.SecurityConfig;
import com.pruebatecnica.solicitudes.dto.MedicamentoResponse;
import com.pruebatecnica.solicitudes.dto.PageResponse;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.exception.GlobalExceptionHandler;
import com.pruebatecnica.solicitudes.exception.MedicamentoNoDisponibleException;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import com.pruebatecnica.solicitudes.notification.EstadoNotificacion;
import com.pruebatecnica.solicitudes.service.AuthenticatedUser;
import com.pruebatecnica.solicitudes.service.MedicamentoService;
import com.pruebatecnica.solicitudes.service.RadicacionService;
import com.pruebatecnica.solicitudes.service.SolicitudService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest({SolicitudController.class, MedicamentoController.class})
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class SolicitudControllerTest {

    private static final String BODY_POS = """
            {"medicamentoId":1}""";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService solicitudService;
    @MockitoBean
    private RadicacionService radicacionService;
    @MockitoBean
    private MedicamentoService medicamentoService;
    @MockitoBean
    private JwtDecoder jwtDecoder; // el filtro real lo necesita; jwt() de spring-security-test simula el token ya validado

    private static RequestPostProcessor usuario(long id) {
        return jwt().jwt(token -> token.subject(String.valueOf(id)).claim("email", "ana@correo.com").claim("nombre", "Ana"));
    }

    @Test
    void sinToken_devuelve401EnFormatoProblemDetail() throws Exception {
        mockMvc.perform(get("/solicitudes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("No autenticado"));

        mockMvc.perform(post("/solicitudes").contentType(MediaType.APPLICATION_JSON).content(BODY_POS))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/medicamentos")).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_usaElIdDelTokenComoUsuario() throws Exception {
        SolicitudResponse creada = new SolicitudResponse(10L, new MedicamentoResponse(1L, "Acetaminofén", true),
                null, null, null, null, Instant.parse("2026-01-01T10:00:00Z"), EstadoNotificacion.ENVIADA);
        when(radicacionService.radicar(any(), any())).thenReturn(creada);

        mockMvc.perform(post("/solicitudes").with(usuario(42)).contentType(MediaType.APPLICATION_JSON).content(BODY_POS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.medicamento.esPos").value(true))
                .andExpect(jsonPath("$.notificacion").value("ENVIADA"));

        // la identidad viaja completa hacia el proceso: id, nombre, correo y token para propagarlo
        ArgumentCaptor<AuthenticatedUser> user = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(radicacionService).radicar(user.capture(), any());
        assertThat(user.getValue().id()).isEqualTo(42L);
        assertThat(user.getValue().email()).isEqualTo("ana@correo.com");
        assertThat(user.getValue().nombre()).isEqualTo("Ana");
        assertThat(user.getValue().bearerToken()).isNotBlank();
    }

    @Test
    void crear_sinEstadoDeNotificacion_omiteElCampoDelJson() throws Exception {
        when(radicacionService.radicar(any(), any())).thenReturn(new SolicitudResponse(11L,
                new MedicamentoResponse(1L, "Acetaminofén", true), null, null, null, null,
                Instant.parse("2026-01-01T10:00:00Z"), null));

        mockMvc.perform(post("/solicitudes").with(usuario(1)).contentType(MediaType.APPLICATION_JSON).content(BODY_POS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificacion").doesNotExist())
                .andExpect(jsonPath("$.numeroOrden").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void crear_sinMedicamento_devuelve400() throws Exception {
        mockMvc.perform(post("/solicitudes").with(usuario(1)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.medicamentoId").value("El medicamento es obligatorio"));
    }

    @Test
    void crear_conFormatoInvalido_devuelve400PorCampo() throws Exception {
        mockMvc.perform(post("/solicitudes").with(usuario(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicamentoId":2,"telefono":"abc","correoContacto":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefono").exists())
                .andExpect(jsonPath("$.errors.correoContacto").exists());
    }

    @Test
    void crear_noPosSinCampos_devuelve400ConDetalle() throws Exception {
        when(radicacionService.radicar(any(), any())).thenThrow(new RequestValidationException(Map.of(
                "numeroOrden", "El número de orden es obligatorio para medicamentos NO POS")));

        mockMvc.perform(post("/solicitudes").with(usuario(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicamentoId":2}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.numeroOrden").exists());
    }

    @Test
    void crear_medicamentoNoDisponible_devuelve422() throws Exception {
        when(radicacionService.radicar(any(), any())).thenThrow(new MedicamentoNoDisponibleException(99L));

        mockMvc.perform(post("/solicitudes").with(usuario(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicamentoId":99}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Medicamento no disponible"));
    }

    @Test
    void listar_pasaPaginacionYUsuarioDelToken() throws Exception {
        when(solicitudService.listar(42L, 2, 5)).thenReturn(new PageResponse<>(List.of(), 2, 5, 11, 3));

        mockMvc.perform(get("/solicitudes").param("page", "2").param("size", "5").with(usuario(42)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listar_conParametroNoNumerico_devuelve400() throws Exception {
        mockMvc.perform(get("/solicitudes").param("page", "abc").with(usuario(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void medicamentos_devuelveElCatalogo() throws Exception {
        when(medicamentoService.listarActivos()).thenReturn(List.of(
                new MedicamentoResponse(1L, "Acetaminofén", true), new MedicamentoResponse(2L, "Adalimumab", false)));

        mockMvc.perform(get("/medicamentos").with(usuario(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].esPos").value(false));
    }
}
