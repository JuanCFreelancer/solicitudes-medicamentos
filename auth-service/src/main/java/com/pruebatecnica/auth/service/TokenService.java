package com.pruebatecnica.auth.service;

import com.pruebatecnica.auth.config.JwtProperties;
import com.pruebatecnica.auth.dto.TokenResponse;
import com.pruebatecnica.auth.entity.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Única responsabilidad: construir y firmar el JWT de un usuario autenticado. */
@Service
public class TokenService {

    private static final String ISSUER = "auth-service";
    private static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenResponse issueFor(Usuario usuario) {
        Instant now = clock.instant();
        Duration lifetime = Duration.ofMinutes(properties.expirationMinutes());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombre())
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, TOKEN_TYPE, lifetime.toSeconds());
    }
}
