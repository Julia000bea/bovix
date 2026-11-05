package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Configuracao;
import com.bovix.model.EventoReproducao;
import com.bovix.model.Fazenda;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReproducaoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final EventoReproducaoRepository repository = mock(EventoReproducaoRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final AnimalService animalService = mock(AnimalService.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private ReproducaoService service;
    private Animal vaca;

    @BeforeEach
    void setUp() {
        service = new ReproducaoService(repository, animalRepository, animalService, fazendaService, configuracaoService, clock);
        vaca = Animal.builder().id(10L).brinco("101").status("Ativa").categoria("Vaca lactante")
                .fazenda(Fazenda.builder().id(1L).build()).build();
        when(animalRepository.findById(10L)).thenReturn(Optional.of(vaca));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(configuracaoService.buscarOuCriar(1L)).thenReturn(Configuracao.builder().gestacaoDias(283).cioDias(21).build());
    }

    private EventoReproducao evento(String tipo, LocalDate data) {
        return EventoReproducao.builder().tipo(tipo).data(data).build();
    }

    @Test
    void inseminacaoCalculaPrevisaoDeParto() {
        EventoReproducao salvo = service.criar(10L, evento("inseminacao", HOJE.minusDays(10)));

        assertThat(salvo.getPrevisaoParto()).isEqualTo(HOJE.minusDays(10).plusDays(283));
    }

    @Test
    void rejeitaDataNoFuturo() {
        assertThatThrownBy(() -> service.criar(10L, evento("cio_detectado", HOJE.plusDays(1))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaTipoInvalido() {
        assertThatThrownBy(() -> service.criar(10L, evento("cio", HOJE)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void diagnosticoExigeResultado() {
        assertThatThrownBy(() -> service.criar(10L, evento("diagnostico_prenhez", HOJE)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void resultadoEhDescartadoForaDoDiagnostico() {
        EventoReproducao dados = evento("parto", HOJE);
        dados.setResultado("prenhe");

        assertThat(service.criar(10L, dados).getResultado()).isNull();
    }

    @Test
    void prePartoDeixaAVacaSeca() {
        EventoReproducao preParto = evento("pre_parto", HOJE);
        when(repository.findByAnimal_IdAndTipoInOrderByDataDesc(eq(10L), anyCollection())).thenReturn(List.of(preParto));

        service.criar(10L, preParto);

        assertThat(vaca.getCategoria()).isEqualTo("Vaca seca");
    }

    @Test
    void eventoRetroativoNaoSobrescreveCategoriaAtual() {
        EventoReproducao partoRecente = evento("parto", HOJE.minusDays(5));
        EventoReproducao secagemAntiga = evento("secagem", HOJE.minusDays(400));
        when(repository.findByAnimal_IdAndTipoInOrderByDataDesc(eq(10L), anyCollection()))
                .thenReturn(List.of(partoRecente, secagemAntiga));

        service.criar(10L, secagemAntiga);

        assertThat(vaca.getCategoria()).isEqualTo("Vaca lactante");
        verify(animalRepository, never()).save(any());
    }

    @Test
    void vinculaCoberturaAoCioDoMesmoAnimal() {
        EventoReproducao cio = EventoReproducao.builder().id(5L).animal(vaca).tipo("cio_confirmado").data(HOJE.minusDays(1)).build();
        when(repository.findById(5L)).thenReturn(Optional.of(cio));
        EventoReproducao dados = evento("inseminacao", HOJE);
        dados.setCioVinculadoId(5L);

        assertThat(service.criar(10L, dados).getCioVinculadoId()).isEqualTo(5L);
    }

    @Test
    void recusaVinculoComCioDeOutroAnimal() {
        Animal outra = Animal.builder().id(99L).build();
        EventoReproducao cio = EventoReproducao.builder().id(5L).animal(outra).tipo("cio_confirmado").data(HOJE).build();
        when(repository.findById(5L)).thenReturn(Optional.of(cio));
        EventoReproducao dados = evento("inseminacao", HOJE);
        dados.setCioVinculadoId(5L);

        assertThatThrownBy(() -> service.criar(10L, dados)).isInstanceOf(RegraNegocioException.class);
    }
}
