package com.pruebatecnica.solicitudes.controller;

import com.pruebatecnica.solicitudes.dto.PageResponse;
import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.service.SolicitudService;
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
@RequestMapping("/solicitudes")
@Tag(name = "Solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una solicitud de medicamento",
            description = "Si el medicamento es NO POS, numeroOrden, direccion, telefono y correoContacto son obligatorios.")
    @ApiResponse(responseCode = "201", description = "Solicitud creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o faltan campos NO POS")
    @ApiResponse(responseCode = "401", description = "Token ausente o inválido")
    @ApiResponse(responseCode = "422", description = "El medicamento no existe o no está disponible")
    public SolicitudResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudRequest request) {
        return solicitudService.crear(userIdOf(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista paginada de las solicitudes del usuario autenticado (más recientes primero)")
    public PageResponse<SolicitudResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número de página, base 0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por página (1-50, por defecto 10)") @RequestParam(required = false) Integer size) {
        return solicitudService.listar(userIdOf(jwt), page, size);
    }

    /** El "sub" del JWT emitido por auth-service es el id del usuario. */
    private static Long userIdOf(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
