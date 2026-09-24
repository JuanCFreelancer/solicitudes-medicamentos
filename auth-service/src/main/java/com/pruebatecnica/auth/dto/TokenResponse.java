package com.pruebatecnica.auth.dto;

/**
 * @param token     JWT firmado
 * @param tipo      esquema para la cabecera Authorization (siempre "Bearer")
 * @param expiraEn  segundos de vigencia del token
 */
public record TokenResponse(String token, String tipo, long expiraEn) {
}
