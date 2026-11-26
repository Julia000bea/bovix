package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.repository.AnimalFotoRepository;
import com.bovix.repository.AnimalRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AnimalServiceDataSaidaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);
    private final AnimalService service = new AnimalService(mock(AnimalRepository.class), mock(AnimalFotoRepository.class),
            mock(FazendaService.class), Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    private Animal animal(String status) {
        return Animal.builder().id(1L).status(status).build();
    }

    @Test
    void vendaSemDataUsaHoje() {
        Animal a = animal("Vendida");
        service.definirDataSaida(a, null, null);
        assertThat(a.getDataSaida()).isEqualTo(HOJE);
    }

    @Test
    void usaADataInformadaEMantemAAnteriorQuandoNaoInformada() {
        Animal a = animal("Morta");
        service.definirDataSaida(a, HOJE.minusDays(3), null);
        assertThat(a.getDataSaida()).isEqualTo(HOJE.minusDays(3));

        service.definirDataSaida(a, null, HOJE.minusDays(3));
        assertThat(a.getDataSaida()).isEqualTo(HOJE.minusDays(3));
    }

    @Test
    void voltarParaAtivaLimpaADataENaoAceitaFuturo() {
        Animal a = animal("Ativa");
        a.setDataSaida(HOJE);
        service.definirDataSaida(a, HOJE, HOJE);
        assertThat(a.getDataSaida()).isNull();

        assertThatThrownBy(() -> service.definirDataSaida(animal("Vendida"), HOJE.plusDays(1), null))
                .isInstanceOf(RegraNegocioException.class);
    }
}
