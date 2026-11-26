package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjecaoLactacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 1, 15);
    private long id = 1;

    private Animal vaca(long animalId) {
        return Animal.builder().id(animalId).status("Ativa").build();
    }

    private EventoReproducao ev(String tipo, LocalDate data) {
        return EventoReproducao.builder().id(id++).tipo(tipo).data(data).build();
    }

    private EventoReproducao prenhe(LocalDate data, LocalDate previsao) {
        EventoReproducao e = ev("diagnostico_prenhez", data);
        e.setResultado("prenhe");
        e.setPrevisaoParto(previsao);
        return e;
    }

    @Test
    void prenheEmLactacaoSecaEVoltaNoParto() {
        // parto previsto 15/06 → secagem 16/04 (60 dias antes)
        LocalDate previsao = LocalDate.of(2026, 6, 15);
        Map<Long, List<EventoReproducao>> eventos = Map.of(1L, List.of(
                ev("parto", HOJE.minusDays(200)),
                ev("inseminacao", HOJE.minusDays(150)),
                prenhe(HOJE.minusDays(110), previsao)));

        var meses = ProjecaoLactacao.projetar(List.of(vaca(1)), eventos, HOJE, 283, 60, 12);

        assertThat(meses).hasSize(12);
        assertThat(meses.get(0).mes()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(mes(meses, 3).emLactacao()).isEqualTo(1);   // março: ainda lactando
        assertThat(mes(meses, 4).secagens()).isEqualTo(1);
        assertThat(mes(meses, 4).emLactacao()).isZero();       // fim de abril: seca
        assertThat(mes(meses, 5).emLactacao()).isZero();
        assertThat(mes(meses, 6).partos()).isEqualTo(1);
        assertThat(mes(meses, 6).emLactacao()).isEqualTo(1);   // volta no parto
        assertThat(meses).allSatisfy(m -> assertThat(m.semPrevisao()).isZero());
    }

    @Test
    void lactanteSemPrenhezFicaSemPrevisaoESecaPrenheEntraNoParto() {
        Map<Long, List<EventoReproducao>> eventos = Map.of(
                1L, List.of(ev("parto", HOJE.minusDays(40))),                         // lactante sem cobertura
                2L, List.of(ev("parto", HOJE.minusDays(400)), ev("inseminacao", HOJE.minusDays(250)),
                        prenhe(HOJE.minusDays(210), LocalDate.of(2026, 3, 10)), ev("secagem", HOJE.minusDays(20))));

        var meses = ProjecaoLactacao.projetar(List.of(vaca(1), vaca(2)), eventos, HOJE, 283, 60, 12);

        assertThat(meses).allSatisfy(m -> assertThat(m.semPrevisao()).isEqualTo(1));
        assertThat(mes(meses, 1).emLactacao()).isZero();
        assertThat(mes(meses, 1).secagens()).isZero();   // já foi secada
        assertThat(mes(meses, 3).partos()).isEqualTo(1);
        assertThat(mes(meses, 3).emLactacao()).isEqualTo(1);
    }

    @Test
    void vendidaNaoEntraNaProjecao() {
        Animal vendida = vaca(1);
        vendida.setStatus("Vendida");
        vendida.setDataSaida(HOJE);
        var meses = ProjecaoLactacao.projetar(List.of(vendida), Map.of(1L, List.of(ev("parto", HOJE.minusDays(10)))),
                HOJE, 283, 60, 3);
        assertThat(meses).allSatisfy(m -> assertThat(m.semPrevisao() + m.emLactacao()).isZero());
    }

    private static ProjecaoLactacao.Mes mes(List<ProjecaoLactacao.Mes> meses, int mes) {
        return meses.stream().filter(m -> m.mes().equals(YearMonth.of(2026, mes))).findFirst().orElseThrow();
    }
}
