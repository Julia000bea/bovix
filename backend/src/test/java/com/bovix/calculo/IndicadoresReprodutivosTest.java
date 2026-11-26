package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresReprodutivosTest {

    private static final LocalDate D = LocalDate.of(2026, 10, 31);
    private long id = 1;

    private Animal vaca(long animalId) {
        return Animal.builder().id(animalId).brinco("B" + animalId).status("Ativa").build();
    }

    private EventoReproducao ev(String tipo, LocalDate data) {
        return EventoReproducao.builder().id(id++).tipo(tipo).data(data).build();
    }

    private EventoReproducao diag(String resultado, LocalDate data) {
        EventoReproducao e = ev("diagnostico_prenhez", data);
        e.setResultado(resultado);
        return e;
    }

    @Test
    void intervaloEntrePartosPorVacaEMediaDoRebanho() {
        // vaca 1: partos com 400 e 380 dias; o primeiro intervalo termina fora da janela
        LocalDate p3 = D.minusDays(20);
        LocalDate p2 = p3.minusDays(380);
        LocalDate p1 = p2.minusDays(400);
        // vaca 2: um intervalo de 420 dias na janela
        LocalDate q2 = D.minusDays(100);
        Map<Long, List<EventoReproducao>> eventos = Map.of(
                1L, List.of(ev("parto", p1), ev("parto", p2), ev("parto", p3)),
                2L, List.of(ev("parto", q2.minusDays(420)), ev("parto", q2)));

        var r = IndicadoresReprodutivos.calcular(List.of(vaca(1), vaca(2)), eventos, D, 283, 90);

        assertThat(r.iepMedio().casos()).isEqualTo(2);
        assertThat(r.iepMedio().valor()).isEqualTo(400.0); // (380 + 420) / 2
        assertThat(r.iepPorVaca()).extracting(IndicadoresReprodutivos.IepVaca::mediaDias).containsExactly(380.0, 420.0);
    }

    @Test
    void partoDuplicadoNaoViraIntervalo() {
        var intervalos = IndicadoresReprodutivos.intervalosEntrePartos(
                List.of(ev("parto", D.minusDays(10)), ev("parto", D.minusDays(8))), D.minusYears(1), D);
        assertThat(intervalos).isEmpty();
    }

    @Test
    void diasEmAbertoETaxaDeConcepcao() {
        LocalDate parto = D.minusDays(200);
        List<EventoReproducao> eventos = List.of(
                ev("parto", parto),
                ev("inseminacao", parto.plusDays(60)),     // falhou: novo cio
                ev("cio_detectado", parto.plusDays(81)),
                ev("inseminacao", parto.plusDays(82)),     // falhou: diagnóstico vazia
                diag("vazia", parto.plusDays(115)),
                ev("inseminacao", parto.plusDays(125)),    // concebeu
                diag("prenhe", parto.plusDays(160)));

        var r = IndicadoresReprodutivos.calcular(List.of(vaca(1)), Map.of(1L, eventos), D, 283, 90);

        assertThat(r.taxaConcepcao().casos()).isEqualTo(3);
        assertThat(r.taxaConcepcao().valor()).isEqualTo(33.3);
        assertThat(r.diasEmAberto().valor()).isEqualTo(125.0);
        assertThat(r.diasEmAberto().casos()).isEqualTo(1);
    }

    @Test
    void coberturaSemDiagnosticoFicaForaDaTaxa() {
        List<EventoReproducao> eventos = List.of(ev("parto", D.minusDays(100)), ev("inseminacao", D.minusDays(10)));
        var r = IndicadoresReprodutivos.calcular(List.of(vaca(1)), Map.of(1L, eventos), D, 283, 90);
        assertThat(r.taxaConcepcao().casos()).isZero();
        assertThat(r.taxaConcepcao().valor()).isNull();
    }

    @Test
    void partoDepoisDaCoberturaConfirmaConcepcao() {
        LocalDate ia = D.minusDays(300);
        var cobs = IndicadoresReprodutivos.coberturas(List.of(ev("inseminacao", ia), ev("parto", ia.plusDays(280))), D);
        assertThat(cobs).singleElement().extracting(IndicadoresReprodutivos.Cobertura::desfecho)
                .isEqualTo(IndicadoresReprodutivos.Desfecho.CONCEBEU);
    }

    @Test
    void perdaGestacionalContaSobrePrenhezesConfirmadasEDeixaAVacaVazia() {
        LocalDate parto = D.minusDays(250);
        List<EventoReproducao> v1 = List.of(
                ev("parto", parto),
                ev("inseminacao", parto.plusDays(70)),
                diag("prenhe", parto.plusDays(105)),
                ev("perda_gestacional", parto.plusDays(150)));
        List<EventoReproducao> v2 = List.of(
                ev("parto", parto),
                ev("inseminacao", parto.plusDays(80)),
                ev("prenhez_confirmada", parto.plusDays(115)));

        var r = IndicadoresReprodutivos.calcular(List.of(vaca(1), vaca(2)), Map.of(1L, v1, 2L, v2), D, 283, 90);

        assertThat(r.perdasGestacionais().quantidade()).isEqualTo(1);
        assertThat(r.perdasGestacionais().prenhezesConfirmadas()).isEqualTo(2);
        assertThat(r.perdasGestacionais().percentual()).isEqualTo(50.0);
        // a perda não desfaz a concepção
        assertThat(r.taxaConcepcao().valor()).isEqualTo(100.0);
        // depois da perda a vaca 1 volta a ser vazia (250 dias pós-parto > 90)
        assertThat(r.vacasVazias()).extracting(IndicadoresReprodutivos.VacaVazia::animalId).containsExactly(1L);
        assertThat(CicloReprodutivo.estado(v1, D, 283).situacao()).isEqualTo(CicloReprodutivo.Situacao.VAZIA);
    }

    @Test
    void vacasVaziasRespeitamXESaida() {
        List<Animal> animais = new ArrayList<>(List.of(vaca(1), vaca(2), vaca(3), vaca(4)));
        animais.get(3).setStatus("Vendida");
        animais.get(3).setDataSaida(D.minusDays(5));
        Map<Long, List<EventoReproducao>> eventos = Map.of(
                1L, List.of(ev("parto", D.minusDays(120))),                                   // vazia há 120
                2L, List.of(ev("parto", D.minusDays(80))),                                    // só 80 dias
                3L, List.of(ev("parto", D.minusDays(150)), ev("inseminacao", D.minusDays(20))), // coberta: aguarda
                4L, List.of(ev("parto", D.minusDays(200))));                                  // vendida

        var vazias = IndicadoresReprodutivos.vacasVazias(animais, eventos, D, 283, 90);

        assertThat(vazias).extracting(IndicadoresReprodutivos.VacaVazia::animalId).containsExactly(1L);
        assertThat(vazias.get(0).diasPosParto()).isEqualTo(120);
    }
}
