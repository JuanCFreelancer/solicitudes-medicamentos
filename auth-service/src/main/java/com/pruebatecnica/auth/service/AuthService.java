package com.pruebatecnica.auth.service;

import com.pruebatecnica.auth.dto.LoginRequest;
import com.pruebatecnica.auth.dto.RegisterRequest;
import com.pruebatecnica.auth.dto.TokenResponse;
import com.pruebatecnica.auth.dto.UsuarioResponse;
import com.pruebatecnica.auth.entity.Usuario;
import com.pruebatecnica.auth.exception.EmailAlreadyRegisteredException;
import com.pruebatecnica.auth.exception.InvalidCredentialsException;
import com.pruebatecnica.auth.repository.UsuarioRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Hash de relleno para que el login tarde lo mismo exista o no el correo (evita enumerar usuarios). */
    private final String dummyHash;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Usuario usuario = new Usuario(request.nombre().trim(), email, passwordEncoder.encode(request.password()));
        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: la restricción UNIQUE de la BD manda.
            throw new EmailAlreadyRegisteredException();
        }
        log.info("Usuario registrado id={}", usuario.getId());
        return UsuarioResponse.from(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(normalizeEmail(request.email())).orElse(null);

        String hashToCompare = usuario != null ? usuario.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCompare);

        if (usuario == null || !passwordMatches) {
            log.warn("Intento de login fallido");
            throw new InvalidCredentialsException();
        }
        log.info("Login exitoso usuarioId={}", usuario.getId());
        return tokenService.issueFor(usuario);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
