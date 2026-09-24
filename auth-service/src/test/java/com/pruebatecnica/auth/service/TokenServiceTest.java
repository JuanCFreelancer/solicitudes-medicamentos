package com.pruebatecnica.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.pruebatecnica.auth.config.JwtProperties;
import com.pruebatecnica.auth.dto.TokenResponse;
import com.pruebatecnica.auth.entity.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica que el token emitido se pueda validar con el mismo secreto (lo que hace solicitudes-service). */
class TokenServiceTest {

    private static final String SECRET = "secreto-de-pruebas-con-mas-de-32-caracteres!!";

    @Test
    void issueFor_generaUnJwtValidoConLosClaimsEsperados() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        TokenService tokenService = new TokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)), new JwtProperties(SECRET, 5), clock);
        Usuario usuario = new Usuario("Ana", "ana@correo.com", "hash");
        ReflectionTestUtils.setField(usuario, "id", 42L);

        TokenResponse response = tokenService.issueFor(usuario);

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@correo.com");
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.expiraEn()).isEqualTo(300);
    }
}
