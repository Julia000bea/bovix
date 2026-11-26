package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.model.Fazenda;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.repository.ProducaoDiariaRepository;
import com.bovix.repository.ProducaoDiariaRepository.ProducaoDia;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class IndicadoresServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 6, 30);

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final EventoReproducaoRepository reproducaoRepository = mock(EventoReproducaoRepository.class);
    private final ProducaoDiariaRepository producaoRepository = mock(ProducaoDiariaRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final IndicadoresService service =
            new IndicadoresService(animalRepository, reproducaoRepository, producaoRepository,
                    mock(ConfiguracaoRepository.class), fazendaService,
                    Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    @Test
    void serieDiariaComDiaSemDadoVazio() {
        Animal vaca = Animal.builder().id(1L).status("Ativa").build();
        EventoReproducao parto = EventoReproducao.builder().animal(vaca).tipo("parto").data(D.minusDays(30)).build();
        when(animalRepository.findByFazenda_IdOrderByBrincoAsc(1L)).thenReturn(List.of(vaca));
        when(reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(1L)).thenReturn(List.of(parto));
        when(producaoRepository.totaisPorDia(1L, D.minusDays(1), D))
                .thenReturn(List.of(new ProducaoDia(D, new BigDecimal("25.00"), true, false)));

        var serie = service.serie(1L, D.minusDays(1), D);

        assertThat(serie).hasSize(2);
        var semDado = serie.get(0);
        assertThat(semDado.litrosTotal()).isNull();
        assertThat(semDado.estimado()).isNull();
        assertThat(semDado.litrosPorVacaEmLactacao()).isNull();
        assertThat(semDado.vacasEmLactacao()).isEqualTo(1);
        var comDado = serie.get(1);
        assertThat(comDado.litrosTotal()).isEqualByComparingTo("25");
        assertThat(comDado.estimado()).isTrue();
        assertThat(comDado.delMedio()).isEqualTo(30.0);
        assertThat(comDado.litrosPorVacaEmLactacao()).isEqualTo(25.0);
    }

    @Test
    void naoAcessaSerieDeOutraFazenda() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));

        assertThatThrownBy(() -> service.serie(2L, D.minusDays(5), D)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(animalRepository, reproducaoRepository, producaoRepository);
    }

    @Test
    void naoAcessaReproducaoNemProjecaoDeOutraFazenda() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));

        assertThatThrownBy(() -> service.reproducao(2L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.projecao(2L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(animalRepository, reproducaoRepository);
    }

    @Test
    void limitaOPeriodo() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(Fazenda.builder().id(1L).build());
        assertThatThrownBy(() -> service.serie(1L, D, D.minusDays(1))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.serie(1L, D.minusDays(IndicadoresService.MAX_DIAS), D))
                .isInstanceOf(RegraNegocioException.class);
    }
}
