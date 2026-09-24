package com.pruebatecnica.auth.dto;

import com.pruebatecnica.auth.entity.Usuario;

/** Vista pública de un usuario: nunca expone el hash de la contraseña. */
public record UsuarioResponse(Long id, String nombre, String email) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
