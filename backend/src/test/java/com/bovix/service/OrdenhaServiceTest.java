package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.OrdenhaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrdenhaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final OrdenhaRepository repository = mock(OrdenhaRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final OrdenhaService service = new OrdenhaService(repository, animalRepository, fazendaService, clock);

    private final Fazenda fazenda = Fazenda.builder().id(1L).build();
    private final Animal vaca = Animal.builder().id(10L).fazenda(fazenda).build();

    @BeforeEach
    void setUp() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(fazenda);
        when(animalRepository.findById(10L)).thenReturn(Optional.of(vaca));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    }

    private Ordenha ordenha(LocalDate data, String litros) {
        return Ordenha.builder().data(data).litros(new BigDecimal(litros)).build();
    }

    @Test
    void bloqueiaRegistroPorAnimalNoDiaQueTemTotalDoRebanho() {
        when(repository.existsByFazenda_IdAndDataAndAnimalIsNull(1L, HOJE)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, 10L, ordenha(HOJE, "20")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("total do rebanho");
        verify(repository, never()).save(any());
    }

    @Test
    void bloqueiaTotalDoRebanhoNoDiaQueTemRegistroPorAnimal() {
        when(repository.existsByFazenda_IdAndDataAndAnimalIsNotNull(1L, HOJE)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, null, ordenha(HOJE, "500")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("por animal");
        verify(repository, never()).save(any());
    }

    @Test
    void permiteVariosRegistrosPorAnimalNoMesmoDia() {
        List<Ordenha> criados = service.criar(1L, 10L, ordenha(HOJE, "12.5"));
        assertThat(criados).hasSize(1);
        assertThat(criados.get(0).getEstimado()).isFalse();
    }

    @Test
    void coletaDeTresDiasDivideOVolumeEMarcaComoEstimado() {
        Ordenha coleta = ordenha(HOJE, "100");
        coleta.setDiasColeta(3);

        List<Ordenha> criados = service.criar(1L, null, coleta);

        assertThat(criados).extracting(Ordenha::getData).containsExactly(HOJE.minusDays(2), HOJE.minusDays(1), HOJE);
        assertThat(criados).extracting(Ordenha::getLitros)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        assertThat(criados).allSatisfy(o -> {
            assertThat(o.getEstimado()).isTrue();
            assertThat(o.getDiasColeta()).isEqualTo(3);
            assertThat(o.getColetaId()).isEqualTo(criados.get(0).getColetaId()).isNotNull();
        });
    }

    @Test
    void coletaNaoCobreDiaJaLancado() {
        when(repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(1L, HOJE.minusDays(1), HOJE))
                .thenReturn(List.of(ordenha(HOJE.minusDays(1), "40")));
        Ordenha coleta = ordenha(HOJE, "100");
        coleta.setDiasColeta(2);

        assertThatThrownBy(() -> service.criar(1L, null, coleta)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void coletaDeVariosDiasNaoValePorAnimal() {
        Ordenha coleta = ordenha(HOJE, "30");
        coleta.setDiasColeta(2);
        assertThatThrownBy(() -> service.criar(1L, 10L, coleta)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaDataNoFuturo() {
        assertThatThrownBy(() -> service.criar(1L, null, ordenha(HOJE.plusDays(1), "10")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void removerParteDeUmaColetaRemoveAColetaInteira() {
        Ordenha parte = Ordenha.builder().id(5L).fazenda(fazenda).data(HOJE).coletaId("abc").build();
        List<Ordenha> coleta = List.of(parte, Ordenha.builder().id(6L).fazenda(fazenda).coletaId("abc").build());
        when(repository.findById(5L)).thenReturn(Optional.of(parte));
        when(repository.findByFazenda_IdAndColetaId(1L, "abc")).thenReturn(coleta);

        service.remover(5L);

        verify(repository).deleteAll(coleta);
    }

    @Test
    void naoAcessaOrdenhaDeOutraFazenda() {
        Fazenda outra = Fazenda.builder().id(2L).build();
        when(repository.findById(7L)).thenReturn(Optional.of(Ordenha.builder().id(7L).fazenda(outra).build()));
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));

        assertThatThrownBy(() -> service.remover(7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.listarPorPeriodo(2L, HOJE, HOJE)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any());
        verify(repository, never()).findByFazenda_IdAndDataBetweenOrderByDataDesc(eq(2L), any(), any());
    }

    @Test
    void animalDeOutraFazendaNaoPodeReceberOrdenha() {
        Animal deOutra = Animal.builder().id(20L).fazenda(Fazenda.builder().id(2L).build()).build();
        when(animalRepository.findById(20L)).thenReturn(Optional.of(deOutra));

        assertThatThrownBy(() -> service.criar(1L, 20L, ordenha(HOJE, "10"))).isInstanceOf(AccessDeniedException.class);
    }
}
