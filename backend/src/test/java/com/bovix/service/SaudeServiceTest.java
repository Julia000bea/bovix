package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoSaude;
import com.bovix.model.Fazenda;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoSaudeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class SaudeServiceTest {

    private final EventoSaudeRepository repository = mock(EventoSaudeRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final SaudeService service = new SaudeService(repository, animalRepository,
            mock(AnimalService.class), fazendaService);

    private Animal animal(long id, String status) {
        return Animal.builder().id(id).brinco(String.valueOf(id)).status(status)
                .fazenda(Fazenda.builder().id(1L).build()).build();
    }

    private EventoSaude vacina() {
        return EventoSaude.builder().tipo("vacina").produto("Aftosa").data(LocalDate.of(2026, 11, 25))
                .proximaAplicacaoDias(365).observacoes("anual").build();
    }

    @Test
    void criaUmRegistroPorAnimalAtivo() {
        when(animalRepository.findByFazenda_IdOrderByBrincoAsc(1L))
                .thenReturn(List.of(animal(1, "Ativa"), animal(2, "Vendida"), animal(3, "Ativa")));
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<EventoSaude> criados = service.criarParaRebanho(1L, vacina());

        verify(fazendaService).verificarPropriedade(1L);
        assertThat(criados).extracting(EventoSaude::getAnimalId).containsExactly(1L, 3L);
        assertThat(criados).allSatisfy(e -> {
            assertThat(e.getProduto()).isEqualTo("Aftosa");
            assertThat(e.getProximaAplicacaoDias()).isEqualTo(365);
            assertThat(e.getId()).isNull();
        });
    }

    @Test
    void semAnimaisAtivosDaErro() {
        when(animalRepository.findByFazenda_IdOrderByBrincoAsc(1L)).thenReturn(List.of(animal(2, "Vendida")));

        assertThatThrownBy(() -> service.criarParaRebanho(1L, vacina()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAll(anyList());
    }
}
