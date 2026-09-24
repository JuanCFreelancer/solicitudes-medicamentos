package com.pruebatecnica.auth.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Ya existe un usuario registrado con ese correo electrónico");
    }
}
