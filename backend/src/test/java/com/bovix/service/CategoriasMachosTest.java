package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoriasMachosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 27);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final Fazenda fazenda = Fazenda.builder().id(1L).build();

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final EventoReproducaoRepository reproducaoRepository = mock(EventoReproducaoRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final AnimalService animalService = new AnimalService(animalRepository, mock(AnimalFotoRepository.class),
            fazendaService, reproducaoRepository, clock);

    private Animal animal(long id, String categoria) {
        return Animal.builder().id(id).brinco(String.valueOf(id)).categoria(categoria).status("Ativa").fazenda(fazenda).build();
    }

    @Test
    void aceitaBezerroETouroERecusaCategoriaInvalida() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(fazenda);
        when(animalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(animalService.criar(1L, animal(1, "Touro")).ehMacho()).isTrue();
        assertThat(animalService.criar(1L, animal(2, "Bezerro")).ehMacho()).isTrue();
        assertThat(animal(3, "Novilha").ehMacho()).isFalse();
        assertThatThrownBy(() -> animalService.criar(1L, animal(4, "Boi"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void femeaComEventosDeReproducaoNaoViraMacho() {
        Animal vaca = animal(5, "Vaca lactante");
        when(animalRepository.findById(5L)).thenReturn(Optional.of(vaca));
        when(animalRepository.existsByIdAndFazenda_Usuario_Id(anyLong(), anyLong())).thenReturn(true);
        when(reproducaoRepository.existsByAnimal_Id(5L)).thenReturn(true);

        AnimalService semDono = spy(animalService);
        doNothing().when(semDono).verificarPropriedade(any());

        assertThatThrownBy(() -> semDono.atualizar(5L, animal(5, "Touro")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("eventos de reprodução");
        assertThat(vaca.getCategoria()).isEqualTo("Vaca lactante");

        // sem eventos, pode corrigir o cadastro
        when(reproducaoRepository.existsByAnimal_Id(5L)).thenReturn(false);
        when(animalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThatCode(() -> semDono.atualizar(5L, animal(5, "Touro"))).doesNotThrowAnyException();
    }

    @Test
    void machoNaoRecebeEventoDeReproducao() {
        Animal touro = animal(7, "Touro");
        when(animalRepository.findById(7L)).thenReturn(Optional.of(touro));
        AnimalService animalSvc = mock(AnimalService.class);
        ReproducaoService reproducao = new ReproducaoService(reproducaoRepository, animalRepository, animalSvc,
                fazendaService, mock(ConfiguracaoService.class), clock);

        assertThatThrownBy(() -> reproducao.criar(7L, EventoReproducao.builder().tipo("cio_detectado").data(HOJE).build()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fêmeas");
        verify(reproducaoRepository, never()).save(any());
    }

    @Test
    void machoNaoRecebeOrdenhaPorAnimal() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(fazenda);
        when(animalRepository.findById(8L)).thenReturn(Optional.of(animal(8, "Bezerro")));
        OrdenhaRepository ordenhaRepository = mock(OrdenhaRepository.class);
        OrdenhaService ordenha = new OrdenhaService(ordenhaRepository, animalRepository, fazendaService, clock);

        assertThatThrownBy(() -> ordenha.criar(1L, 8L, Ordenha.builder().data(HOJE).litros(BigDecimal.TEN).build()))
                .isInstanceOf(RegraNegocioException.class);
        verify(ordenhaRepository, never()).save(any());
    }
}
