package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresRebanhoTest {

    private static final LocalDate D = LocalDate.of(2026, 6, 30);

    private static Animal vaca(long id) {
        return Animal.builder().id(id).brinco(String.valueOf(id)).status("Ativa").build();
    }

    private static EventoReproducao ev(String tipo, LocalDate data) {
        return EventoReproducao.builder().tipo(tipo).data(data).build();
    }

    @Test
    void vacaComPartoSemSecagemEstaEmLactacao() {
        var eventos = List.of(ev("parto", D.minusDays(100)));
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), eventos, D)).isTrue();
        assertThat(IndicadoresRebanho.del(eventos, D)).isEqualTo(100);
    }

    @Test
    void partoDepoisDeDNaoConta() {
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), List.of(ev("parto", D.plusDays(1))), D)).isFalse();
    }

    @Test
    void secagemDepoisDoUltimoPartoEncerraALactacao() {
        var eventos = List.of(ev("parto", D.minusDays(400)), ev("secagem", D.minusDays(60)));
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), eventos, D)).isFalse();
        // antes da secagem ela estava em lactação
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), eventos, D.minusDays(61))).isTrue();
    }

    @Test
    void novoPartoDepoisDaSecagemReiniciaALactacao() {
        var eventos = List.of(ev("parto", D.minusDays(400)), ev("secagem", D.minusDays(80)), ev("parto", D.minusDays(10)));
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), eventos, D)).isTrue();
        assertThat(IndicadoresRebanho.del(eventos, D)).isEqualTo(10);
    }

    @Test
    void prePartoTambemMarcaVacaSeca() {
        var eventos = List.of(ev("parto", D.minusDays(330)), ev("pre_parto", D.minusDays(5)));
        assertThat(IndicadoresRebanho.emLactacao(vaca(1), eventos, D)).isFalse();
    }

    @Test
    void vendaOuMorteAntesDeDTiraDaContaMasNaoApagaOHistorico() {
        Animal vendida = vaca(1);
        vendida.setStatus("Vendida");
        vendida.setDataSaida(D.minusDays(10));
        Animal morta = vaca(2);
        morta.setStatus("Morta");
        morta.setDataSaida(D);
        var eventos = List.of(ev("parto", D.minusDays(100)));

        assertThat(IndicadoresRebanho.emLactacao(vendida, eventos, D)).isFalse();
        assertThat(IndicadoresRebanho.emLactacao(vendida, eventos, D.minusDays(10))).isTrue(); // no dia da saída ainda conta
        assertThat(IndicadoresRebanho.emLactacao(morta, eventos, D)).isTrue();
        assertThat(IndicadoresRebanho.emLactacao(morta, eventos, D.plusDays(1))).isFalse();
    }

    @Test
    void lactacaoDoRebanhoComDelMedioELitrosPorVaca() {
        Map<Long, List<EventoReproducao>> eventos = Map.of(
                1L, List.of(ev("parto", D.minusDays(100))),
                2L, List.of(ev("parto", D.minusDays(51))),
                3L, List.of(ev("parto", D.minusDays(300)), ev("secagem", D.minusDays(1))));
        var l = IndicadoresRebanho.lactacao(List.of(vaca(1), vaca(2), vaca(3), vaca(4)), eventos, D);

        assertThat(l.vacasEmLactacao()).isEqualTo(2);
        assertThat(l.delMedio()).isEqualTo(75.5);
        assertThat(IndicadoresRebanho.litrosPorVaca(new BigDecimal("41"), 2)).isEqualTo(20.5);
        assertThat(IndicadoresRebanho.litrosPorVaca(null, 2)).isNull();
        assertThat(IndicadoresRebanho.litrosPorVaca(BigDecimal.TEN, 0)).isNull();
    }

    @Test
    void semVacasEmLactacaoDelFicaVazio() {
        var l = IndicadoresRebanho.lactacao(List.of(vaca(1)), Map.of(), D);
        assertThat(l.vacasEmLactacao()).isZero();
        assertThat(l.delMedio()).isNull();
    }
}
