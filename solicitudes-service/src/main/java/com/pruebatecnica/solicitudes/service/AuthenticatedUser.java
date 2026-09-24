package com.pruebatecnica.solicitudes.service;

/**
 * Identidad del llamador, extraída del JWT en la capa web para que los servicios no dependan de Spring Security.
 *
 * @param bearerToken JWT original, necesario para propagar la identidad a otros servicios
 */
public record AuthenticatedUser(Long id, String nombre, String email, String bearerToken) {
}
