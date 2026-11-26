package com.bovix.scheduler;

import com.bovix.clima.ClimaDiario;
import com.bovix.clima.FonteClimaException;
import com.bovix.clima.InmetFonteClima;
import com.bovix.repository.ClimaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Busca no INMET o clima dos dias que faltam (até ontem, nos últimos {@link #DIAS_RECUPERAR} dias)
 * de cada estação configurada nas fazendas.
 *
 * - Idempotente: só pede os dias que não estão no banco e grava com ON CONFLICT DO NOTHING.
 * - Recupera dias faltantes: se a API dormiu ou o INMET falhou, os dias perdidos entram na próxima vez.
 * - Nunca derruba a aplicação: qualquer falha é registrada no log e tentada de novo na próxima hora.
 * - Roda a cada hora, mas só consulta o banco/INMET uma vez por dia depois de dar certo (o Neon pode dormir).
 */
@Component
@RequiredArgsConstructor
public class ClimaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClimaScheduler.class);
    static final int DIAS_RECUPERAR = 30;

    private final InmetFonteClima inmet;
    private final ClimaRepository climaRepository;
    private final Clock clock;

    private volatile LocalDate sincronizadoEm;
    private volatile boolean avisouSemToken;

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    public void executar() {
        LocalDate hoje = LocalDate.now(clock);
        if (hoje.equals(sincronizadoEm)) return;
        if (!inmet.disponivel()) {
            if (!avisouSemToken) {
                log.info("Clima: BOVIX_INMET_TOKEN não configurado; busca automática no INMET desligada (use a importação de CSV).");
                avisouSemToken = true;
            }
            return;
        }
        try {
            if (sincronizar(hoje)) sincronizadoEm = hoje;
        } catch (Exception e) {
            log.warn("Clima: falha na sincronização diária ({}). Nova tentativa em 1 hora.", e.getMessage());
        }
    }

    /** Busca os dias faltantes de todas as estações. Devolve true se nenhuma estação falhou. */
    boolean sincronizar(LocalDate hoje) {
        LocalDate ontem = hoje.minusDays(1);
        LocalDate inicio = hoje.minusDays(DIAS_RECUPERAR);
        boolean tudoCerto = true;
        for (String estacao : climaRepository.estacoesEmUso()) {
            try {
                int gravados = sincronizarEstacao(estacao, inicio, ontem);
                if (gravados > 0) log.info("Clima: {} dia(s) gravados para a estação {}", gravados, estacao);
            } catch (FonteClimaException | RuntimeException e) {
                tudoCerto = false;
                log.warn("Clima: estação {} não atualizada: {}", estacao, e.getMessage());
            }
        }
        return tudoCerto;
    }

    int sincronizarEstacao(String estacao, LocalDate inicio, LocalDate fim) throws FonteClimaException {
        Set<LocalDate> existentes = climaRepository.datasExistentes(estacao, inicio, fim);
        List<LocalDate> faltando = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (!existentes.contains(d)) faltando.add(d);
        }
        if (faltando.isEmpty()) return 0;

        int gravados = 0;
        for (ClimaDiario dia : inmet.buscar(estacao, faltando.get(0), faltando.get(faltando.size() - 1))) {
            if (faltando.contains(dia.data()) && climaRepository.inserirSeNaoExiste(dia)) gravados++;
        }
        return gravados;
    }
}
