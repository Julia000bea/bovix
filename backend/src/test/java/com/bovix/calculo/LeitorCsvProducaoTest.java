package com.bovix.calculo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorCsvProducaoTest {

    @Test
    void aceitaOsDoisFormatosDeDataESeparadores() {
        var r = LeitorCsvProducao.ler("""
                data;litros
                2026-01-05;1234,5
                06/01/2026;1200.25
                2026-01-07,1100
                "2026-01-08","1.050,75"
                2026-01-09,980,5
                """);

        assertThat(r.rejeitadas()).isEmpty();
        assertThat(r.validas()).extracting(LeitorCsvProducao.LinhaProducao::data).containsExactly(
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7),
                LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9));
        assertThat(r.validas()).extracting(LeitorCsvProducao.LinhaProducao::litros).containsExactly(
                new BigDecimal("1234.50"), new BigDecimal("1200.25"), new BigDecimal("1100.00"),
                new BigDecimal("1050.75"), new BigDecimal("980.50"));
    }

    @Test
    void rejeitaComMotivoENumeroDaLinha() {
        var r = LeitorCsvProducao.ler("""
                2026-01-05;100
                2026-02-30;100
                05/01/2026;200
                2026-01-06;abc
                2026-01-07;-5
                2026-01-08
                """);

        assertThat(r.validas()).hasSize(1);
        assertThat(r.rejeitadas()).extracting(LeitorCsvProducao.LinhaRejeitada::linha).containsExactly(2, 3, 4, 5, 6);
        assertThat(r.rejeitadas().get(0).motivo()).contains("data inválida");
        assertThat(r.rejeitadas().get(1).motivo()).contains("repetida").contains("linha 1");
        assertThat(r.rejeitadas().get(2).motivo()).contains("litros inválido");
        assertThat(r.rejeitadas().get(3).motivo()).contains("fora do intervalo");
        assertThat(r.rejeitadas().get(4).motivo()).contains("2 colunas");
    }

    @Test
    void ignoraBomLinhasEmBrancoECabecalho() {
        var r = LeitorCsvProducao.ler("﻿Data,Litros\r\n\r\n2026-03-01,10\r\n");
        assertThat(r.validas()).hasSize(1);
        assertThat(r.rejeitadas()).isEmpty();
    }

    @Test
    void limitaNumeroDeLinhas() {
        StringBuilder sb = new StringBuilder();
        LocalDate d = LocalDate.of(2000, 1, 1);
        for (int i = 0; i <= LeitorCsvProducao.MAX_LINHAS + 1; i++) sb.append(d.plusDays(i)).append(";10\n");
        assertThatThrownBy(() -> LeitorCsvProducao.ler(sb.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5000");
    }

    @Test
    void numerosComMilharEDecimal() {
        assertThat(LeitorCsvProducao.lerNumero("1.234,5")).isEqualByComparingTo("1234.5");
        assertThat(LeitorCsvProducao.lerNumero("1,234.5")).isEqualByComparingTo("1234.5");
        assertThat(LeitorCsvProducao.lerNumero("12,5")).isEqualByComparingTo("12.5");
        assertThat(LeitorCsvProducao.lerNumero("1,2,3")).isNull();
    }
}
