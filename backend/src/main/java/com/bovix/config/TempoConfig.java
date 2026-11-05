package com.bovix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio da aplicação no fuso da fazenda (padrão America/Sao_Paulo).
 * O container no Render roda em UTC: sem isto, "hoje" vira amanhã depois das 21h
 * e o e-mail das 07:00 sairia às 04:00.
 */
@Configuration
public class TempoConfig {

    @Bean
    public Clock clock(@Value("${bovix.fuso-horario}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
