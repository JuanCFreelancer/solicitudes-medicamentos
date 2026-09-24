package com.pruebatecnica.auth.exception;

/** Mensaje deliberadamente genérico: no revela si falló el correo o la contraseña. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
