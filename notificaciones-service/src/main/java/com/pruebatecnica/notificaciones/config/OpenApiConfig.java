package com.pruebatecnica.notificaciones.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI notificacionesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notificaciones Service API")
                        .version("1.0.0")
                        .description("Servicio fino y reutilizable para enviar y consultar notificaciones. Requiere el JWT emitido por auth-service."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
