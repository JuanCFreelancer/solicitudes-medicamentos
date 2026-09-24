package com.pruebatecnica.auth.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Reloj inyectable: permite fijar la hora en los tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
