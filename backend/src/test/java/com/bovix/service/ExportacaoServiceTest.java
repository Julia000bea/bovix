package com.bovix.service;

import com.bovix.clima.ClimaDiario;
import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.model.Fazenda;
import com.bovix.model.Ocorrencia;
import com.bovix.repository.*;
import com.bovix.repository.ProducaoDiariaRepository.ProducaoDia;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ExportacaoServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 6, 30);

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final EventoReproducaoRepository reproducaoRepository = mock(EventoReproducaoRepository.class);
    private final ProducaoDiariaRepository producaoRepository = mock(ProducaoDiariaRepository.class);
    private final ConfiguracaoRepository configuracaoRepository = mock(ConfiguracaoRepository.class);
    private final ClimaRepository climaRepository = mock(ClimaRepository.class);
    private final OcorrenciaRepository ocorrenciaRepository = mock(OcorrenciaRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final IndicadoresService indicadoresService = new IndicadoresService(animalRepository, reproducaoRepository,
            producaoRepository, configuracaoRepository, fazendaService,
            Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    private final ExportacaoService service = new ExportacaoService(indicadoresService, producaoRepository,
            climaRepository, ocorrenciaRepository, fazendaService);

    @Test
    void umaLinhaPorDiaComVazioQuandoNaoHaDado() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(Fazenda.builder().id(1L).estacaoInmet("A001").build());
        when(configuracaoRepository.findByFazenda_Id(anyLong())).thenReturn(Optional.empty());
        Animal vaca = Animal.builder().id(1L).brinco("1").status("Ativa").build();
        when(animalRepository.findByFazenda_IdOrderByBrincoAsc(1L)).thenReturn(List.of(vaca));
        when(reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(1L)).thenReturn(List.of(
                EventoReproducao.builder().id(1L).animal(vaca).tipo("parto").data(D.minusDays(500)).build(),
                EventoReproducao.builder().id(2L).animal(vaca).tipo("parto").data(D.minusDays(100)).build(),
                EventoReproducao.builder().id(3L).animal(vaca).tipo("perda_gestacional").data(D.minusDays(1)).build()));
        when(producaoRepository.totaisPorDia(1L, D.minusDays(1), D))
                .thenReturn(List.of(new ProducaoDia(D, new BigDecimal("24.50"), false, false)));
        when(climaRepository.serie("A001", D.minusDays(1), D))
                .thenReturn(List.of(new ClimaDiario("A001", D.minusDays(1), 30.0, 33.0, 25.0, 70.0, 81.38, 84.0, 9, "inmet")));
        when(ocorrenciaRepository.findByFazenda_IdAndDataBetweenOrderByDataDesc(1L, D.minusDays(1), D)).thenReturn(List.of(
                Ocorrencia.builder().data(D).tipo("agua").build(), Ocorrencia.builder().data(D).tipo("equipamento").build()));

        String[] linhas = service.exportarCsv(1L, D.minusDays(1), D).split("\n");

        assertThat(linhas[0]).isEqualTo(ExportacaoService.CABECALHO);
        assertThat(linhas).hasSize(3);
        // ontem: sem ordenha (vazio), com clima; vaca vazia há 99 dias (> 90)
        assertThat(linhas[1]).isEqualTo("2026-06-29,,,1,99,,81.38,84,9,400,1,1,");
        // hoje: com ordenha, sem clima, duas ocorrências
        assertThat(linhas[2]).isEqualTo("2026-06-30,24.5,0,1,100,24.5,,,,400,1,1,agua|equipamento");
    }

    @Test
    void naoExportaFazendaDeOutroUsuario() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));
        assertThatThrownBy(() -> service.exportarCsv(2L, D, D)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(animalRepository, producaoRepository, climaRepository, ocorrenciaRepository);
    }
}
