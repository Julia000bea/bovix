package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.model.Ocorrencia;
import com.bovix.repository.OcorrenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OcorrenciaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final OcorrenciaRepository repository = mock(OcorrenciaRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final OcorrenciaService service = new OcorrenciaService(repository, fazendaService,
            Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    private final Fazenda minha = Fazenda.builder().id(1L).build();
    private final Fazenda outra = Fazenda.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(minha);
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Ocorrencia ocorrencia(String tipo, LocalDate data) {
        return Ocorrencia.builder().tipo(tipo).data(data).descricao("  Bebedouro vazio de manhã ").build();
    }

    @Test
    void criaComDescricaoLimpa() {
        Ocorrencia criada = service.criar(1L, ocorrencia("agua", HOJE));
        assertThat(criada.getFazenda()).isSameAs(minha);
        assertThat(criada.getDescricao()).isEqualTo("Bebedouro vazio de manhã");
    }

    @Test
    void validaTipoEData() {
        assertThatThrownBy(() -> service.criar(1L, ocorrencia("festa", HOJE))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.criar(1L, ocorrencia("agua", HOJE.plusDays(1)))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoLeNemAlteraOcorrenciaDeOutraFazenda() {
        Ocorrencia alheia = Ocorrencia.builder().id(9L).fazenda(outra).tipo("agua").data(HOJE).descricao("x").build();
        when(repository.findById(9L)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> service.listar(2L, HOJE, HOJE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.criar(2L, ocorrencia("agua", HOJE))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.atualizar(9L, ocorrencia("clima", HOJE))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.remover(9L)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
        assertThat(alheia.getTipo()).isEqualTo("agua");
    }

    @Test
    void atualizaAPropria() {
        Ocorrencia propria = Ocorrencia.builder().id(5L).fazenda(minha).tipo("agua").data(HOJE).descricao("x").build();
        when(repository.findById(5L)).thenReturn(Optional.of(propria));

        service.atualizar(5L, ocorrencia("equipamento", HOJE.minusDays(1)));

        assertThat(propria.getTipo()).isEqualTo("equipamento");
        assertThat(propria.getData()).isEqualTo(HOJE.minusDays(1));
    }
}
