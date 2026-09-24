package com.pruebatecnica.notificaciones.controller;

import com.pruebatecnica.notificaciones.dto.NotificacionRequest;
import com.pruebatecnica.notificaciones.dto.NotificacionResponse;
import com.pruebatecnica.notificaciones.dto.PageResponse;
import com.pruebatecnica.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificaciones")
@Tag(name = "Notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envía una notificación y registra el resultado",
            description = "Devuelve 201 aunque el canal falle: el resultado queda en el campo estado (ENVIADA o FALLIDA).")
    @ApiResponse(responseCode = "201", description = "Notificación registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Token ausente o inválido")
    public NotificacionResponse enviar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NotificacionRequest request) {
        return notificacionService.enviar(userIdOf(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista paginada de las notificaciones del usuario autenticado (más recientes primero)")
    public PageResponse<NotificacionResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número de página, base 0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por página (1-50, por defecto 10)") @RequestParam(required = false) Integer size) {
        return notificacionService.listar(userIdOf(jwt), page, size);
    }

    /** El "sub" del JWT emitido por auth-service es el id del usuario. */
    private static Long userIdOf(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
