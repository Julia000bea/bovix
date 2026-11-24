package com.bovix.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/health — público e propositalmente sem consultar o banco.
 * Serve para o "health check" da hospedagem e para um serviço externo (ex.: cron-job.org)
 * manter a API acordada no plano grátis do Render sem manter o banco do Neon ligado.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
