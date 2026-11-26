package com.bovix.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita quantas contas um mesmo IP cria por hora (contra cadastro automatizado em massa).
 * Fica em memória (uma instância só, como no Render): reiniciar a API zera os contadores.
 * É uma barreira contra abuso simples, não uma garantia: IPs compartilhados (mesma rede) dividem o limite.
 */
@Component
public class LimiteCadastros {

    static final int MAX_POR_IP = 3;
    static final Duration JANELA = Duration.ofHours(1);
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private record Contagem(int contas, Instant inicio) {}

    private final Map<String, Contagem> porIp = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteCadastros(Clock clock) {
        this.clock = clock;
    }

    /** Lança MuitosCadastrosException se o IP já criou o máximo de contas na janela. */
    public void verificar(String ip) {
        Contagem c = porIp.get(ip);
        if (c != null && !expirou(c) && c.contas() >= MAX_POR_IP) {
            throw new MuitosCadastrosException();
        }
    }

    /** Conta uma conta criada por este IP. */
    public void registrar(String ip) {
        if (porIp.size() > LIMPAR_ACIMA_DE) porIp.values().removeIf(this::expirou);
        porIp.compute(ip, (k, c) -> c == null || expirou(c)
                ? new Contagem(1, Instant.now(clock))
                : new Contagem(c.contas() + 1, c.inicio()));
    }

    private boolean expirou(Contagem c) {
        return !c.inicio().plus(JANELA).isAfter(Instant.now(clock));
    }

    /**
     * IP do cliente. No Render a API fica atrás de um proxy que ACRESCENTA o IP real no fim do
     * X-Forwarded-For; o primeiro valor pode ter sido inventado pelo próprio cliente, então usamos o último.
     */
    public static String ipDoCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String[] ips = encaminhado.split(",");
            String ultimo = ips[ips.length - 1].trim();
            if (!ultimo.isEmpty()) return ultimo;
        }
        return request.getRemoteAddr();
    }

    public static class MuitosCadastrosException extends RuntimeException {
        public MuitosCadastrosException() {
            super("Muitas contas criadas a partir desta rede. Tente novamente mais tarde.");
        }
    }
}
