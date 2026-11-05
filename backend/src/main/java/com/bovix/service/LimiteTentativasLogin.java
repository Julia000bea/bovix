package com.bovix.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção simples contra força bruta no login: após MAX_FALHAS senhas erradas para o mesmo
 * e-mail dentro da janela, novas tentativas ficam bloqueadas até a janela expirar.
 * Fica em memória (uma instância só, como no Render); reiniciar a API zera os contadores.
 */
@Component
public class LimiteTentativasLogin {

    private static final int MAX_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private record Tentativas(int falhas, Instant inicio) {}

    private final Map<String, Tentativas> porEmail = new ConcurrentHashMap<>();

    public void verificar(String email) {
        Tentativas t = porEmail.get(email);
        if (t == null) return;
        if (expirou(t)) {
            porEmail.remove(email, t);
            return;
        }
        if (t.falhas() >= MAX_FALHAS) {
            throw new MuitasTentativasException();
        }
    }

    public void registrarFalha(String email) {
        porEmail.compute(email, (k, t) -> (t == null || expirou(t))
                ? new Tentativas(1, Instant.now())
                : new Tentativas(t.falhas() + 1, t.inicio()));
    }

    public void registrarSucesso(String email) {
        porEmail.remove(email);
    }

    private boolean expirou(Tentativas t) {
        return t.inicio().plus(JANELA).isBefore(Instant.now());
    }

    public static class MuitasTentativasException extends RuntimeException {
        public MuitasTentativasException() {
            super("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.");
        }
    }
}
