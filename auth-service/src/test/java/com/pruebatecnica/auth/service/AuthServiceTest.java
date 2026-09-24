package com.pruebatecnica.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pruebatecnica.auth.dto.LoginRequest;
import com.pruebatecnica.auth.dto.RegisterRequest;
import com.pruebatecnica.auth.dto.TokenResponse;
import com.pruebatecnica.auth.dto.UsuarioResponse;
import com.pruebatecnica.auth.entity.Usuario;
import com.pruebatecnica.auth.exception.EmailAlreadyRegisteredException;
import com.pruebatecnica.auth.exception.InvalidCredentialsException;
import com.pruebatecnica.auth.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenService tokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // costo bajo: solo para tests
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, tokenService);
    }

    @Test
    void register_guardaElPasswordHasheadoYElCorreoEnMinusculas() {
        when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(false);

        UsuarioResponse response = authService.register(new RegisterRequest("  Ana  ", "Ana@Correo.com", "Clave1234"));

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@correo.com");
        assertThat(saved.getValue().getNombre()).isEqualTo("Ana");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Clave1234");
        assertThat(passwordEncoder.matches("Clave1234", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("ana@correo.com");
    }

    @Test
    void register_conCorreoExistente_lanzaConflicto() {
        when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ana", "ana@correo.com", "Clave1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_conCarreraEnLaBd_tambienLanzaConflicto() {
        when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_usuarios_email"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ana", "ana@correo.com", "Clave1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void login_conCredencialesCorrectas_devuelveToken() {
        Usuario usuario = new Usuario("Ana", "ana@correo.com", passwordEncoder.encode("Clave1234"));
        TokenResponse esperado = new TokenResponse("jwt", "Bearer", 300);
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenService.issueFor(usuario)).thenReturn(esperado);

        TokenResponse response = authService.login(new LoginRequest(" ANA@correo.com ", "Clave1234"));

        assertThat(response).isSameAs(esperado);
    }

    @Test
    void login_conPasswordIncorrecto_lanzaCredencialesInvalidas() {
        Usuario usuario = new Usuario("Ana", "ana@correo.com", passwordEncoder.encode("Clave1234"));
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@correo.com", "otra-clave")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).issueFor(any());
    }

    @Test
    void login_conCorreoInexistente_lanzaElMismoError() {
        when(usuarioRepository.findByEmail("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@correo.com", "Clave1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }
}
