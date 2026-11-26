package com.bovix.scheduler;

import com.bovix.clima.ClimaDiario;
import com.bovix.clima.FonteClimaException;
import com.bovix.clima.InmetFonteClima;
import com.bovix.repository.ClimaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClimaSchedulerTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);
    private static final LocalDate ONTEM = HOJE.minusDays(1);

    private final InmetFonteClima inmet = mock(InmetFonteClima.class);
    private final ClimaRepository repo = mock(ClimaRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final ClimaScheduler scheduler = new ClimaScheduler(inmet, repo, clock);
    private final Set<LocalDate> gravados = new HashSet<>();

    @BeforeEach
    void setUp() {
        when(inmet.disponivel()).thenReturn(true);
        when(repo.estacoesEmUso()).thenReturn(List.of("A001"));
        // banco simulado: datasExistentes devolve o que já foi gravado
        when(repo.datasExistentes(eq("A001"), any(), any())).thenAnswer(i -> new HashSet<>(gravados));
        when(repo.inserirSeNaoExiste(any())).thenAnswer(i -> gravados.add(((ClimaDiario) i.getArgument(0)).data()));
    }

    private static ClimaDiario dia(LocalDate d) {
        return new ClimaDiario("A001", d, 25.0, 30.0, 20.0, 70.0, 74.0, 80.0, 6, "inmet");
    }

    @Test
    void buscaSoOsDiasQueFaltamEERodarDeNovoNaoDuplica() throws Exception {
        // 28 dos 30 dias já estão no banco; faltam ontem e anteontem
        for (LocalDate d = HOJE.minusDays(ClimaScheduler.DIAS_RECUPERAR); d.isBefore(ONTEM.minusDays(1)); d = d.plusDays(1)) {
            gravados.add(d);
        }
        when(inmet.buscar("A001", ONTEM.minusDays(1), ONTEM)).thenReturn(List.of(dia(ONTEM.minusDays(1)), dia(ONTEM)));

        assertThat(scheduler.sincronizar(HOJE)).isTrue();
        assertThat(gravados).contains(ONTEM, ONTEM.minusDays(1));

        // segunda rodada: nada faltando, o INMET nem é chamado
        clearInvocations(inmet);
        assertThat(scheduler.sincronizar(HOJE)).isTrue();
        verify(inmet, never()).buscar(any(), any(), any());
    }

    @Test
    void executarDuasVezesNoMesmoDiaSoSincronizaUma() throws Exception {
        when(inmet.buscar(any(), any(), any())).thenReturn(List.of());
        scheduler.executar();
        scheduler.executar();
        verify(repo, times(1)).estacoesEmUso();
    }

    @Test
    void falhaDaFonteNaoDerrubaETentaDeNovo() throws Exception {
        when(inmet.buscar(any(), any(), any())).thenThrow(new FonteClimaException("INMET fora do ar"));

        assertThatCode(scheduler::executar).doesNotThrowAnyException();
        assertThatCode(scheduler::executar).doesNotThrowAnyException();
        // como falhou, a segunda chamada tenta de novo (não marcou o dia como sincronizado)
        verify(inmet, times(2)).buscar(any(), any(), any());
    }

    @Test
    void umaEstacaoComFalhaNaoImpedeAsOutras() throws Exception {
        when(repo.estacoesEmUso()).thenReturn(List.of("A001", "B002"));
        when(repo.datasExistentes(eq("B002"), any(), any())).thenReturn(Set.of());
        when(inmet.buscar(eq("A001"), any(), any())).thenThrow(new RuntimeException("timeout"));
        when(inmet.buscar(eq("B002"), any(), any())).thenReturn(List.of(
                new ClimaDiario("B002", ONTEM, 25.0, 30.0, 20.0, 70.0, 74.0, 80.0, 6, "inmet")));

        assertThat(scheduler.sincronizar(HOJE)).isFalse();
        verify(repo).inserirSeNaoExiste(argThat(d -> d.estacao().equals("B002")));
    }

    @Test
    void semTokenNaoConsultaNada() {
        when(inmet.disponivel()).thenReturn(false);
        scheduler.executar();
        verifyNoInteractions(repo);
    }

    @Test
    void erroDoBancoTambemNaoDerruba() {
        when(repo.estacoesEmUso()).thenThrow(new RuntimeException("banco fora"));
        assertThatCode(scheduler::executar).doesNotThrowAnyException();
    }
}
