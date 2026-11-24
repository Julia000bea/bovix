package com.bovix.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simples de "no máximo N vezes dentro da janela" por chave (ex.: e-mail, fazenda).
 * Usado em ações que disparam e-mail, para ninguém usar o Bovix para mandar spam
 * pela conta SMTP da fazenda. Fica em memória: reiniciar a API zera os contadores.
 */
@Component
public class LimiteEnvios {

    // acima disso, descarta chaves antigas para o mapa não crescer sem limite
    private static final int MAX_CHAVES = 10_000;

    private final Map<String, Deque<Instant>> registros = new ConcurrentHashMap<>();

    /** Registra a tentativa e devolve true se ainda está dentro do limite. */
    public boolean permitir(String chave, int maximo, Duration janela) {
        Instant agora = Instant.now();
        Instant limite = agora.minus(janela);
        if (registros.size() > MAX_CHAVES) {
            registros.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    Instant ultimo = e.getValue().peekLast();
                    return ultimo == null || ultimo.isBefore(limite);
                }
            });
        }
        Deque<Instant> fila = registros.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst().isBefore(limite)) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                return false;
            }
            fila.addLast(agora);
            return true;
        }
    }
}
