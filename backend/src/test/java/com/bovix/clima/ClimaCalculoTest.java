package com.bovix.clima;

import com.bovix.calculo.Itu;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClimaCalculoTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Test
    void formulaDoItu() {
        assertThat(Itu.calcular(30, 70)).isCloseTo(81.38, within(0.005));
        // (1,8·20 + 32) − (0,55 − 0,0055·60)·(1,8·20 − 26) = 68 − 0,22·10
        assertThat(Itu.calcular(20, 60)).isCloseTo(65.80, within(0.01));
    }

    @Test
    void agregaPorHoraNoFusoLocal() {
        // 24 horas do dia 10/01 em Brasília = 03:00 UTC de 10/01 até 02:00 UTC de 11/01
        List<LeituraHoraria> horas = new ArrayList<>();
        Instant inicio = Instant.parse("2026-01-10T03:00:00Z");
        for (int h = 0; h < 24; h++) {
            double t = h < 12 ? 20 : 30; // 12 horas a 20 °C e 12 a 30 °C
            horas.add(new LeituraHoraria(inicio.plusSeconds(3600L * h), t, 70.0, t + 1, t - 1));
        }
        // uma leitura de 02:00 UTC do dia 10 é do dia 09 local e fica de fora (dia incompleto)
        horas.add(new LeituraHoraria(Instant.parse("2026-01-10T02:00:00Z"), 40.0, 10.0, null, null));

        List<ClimaDiario> dias = AgregadorClima.agregar(horas, "A001", "inmet", SP);

        assertThat(dias).hasSize(1);
        ClimaDiario d = dias.get(0);
        assertThat(d.data()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(d.tempMedia()).isEqualTo(25.0);
        assertThat(d.tempMax()).isEqualTo(31.0);
        assertThat(d.tempMin()).isEqualTo(19.0);
        assertThat(d.umidadeMedia()).isEqualTo(70.0);
        assertThat(d.ituMax()).isEqualTo(81.38);
        assertThat(d.ituMedio()).isCloseTo((Itu.calcular(20, 70) + Itu.calcular(30, 70)) / 2, within(0.01));
        assertThat(d.horasItuAcima72()).isEqualTo(12);
    }

    @Test
    void diaComMenosDe18HorasNaoEntra() {
        List<LeituraHoraria> horas = new ArrayList<>();
        for (int h = 0; h < 17; h++) {
            horas.add(new LeituraHoraria(Instant.parse("2026-01-10T03:00:00Z").plusSeconds(3600L * h), 25.0, 60.0, null, null));
        }
        assertThat(AgregadorClima.agregar(horas, "A001", "inmet", SP)).isEmpty();
    }

    @Test
    void leArquivoHorarioDoInmet() throws Exception {
        StringBuilder csv = new StringBuilder("""
                REGIAO:;CO
                UF:;DF
                ESTACAO:;BRASILIA
                CODIGO (WMO):;A001
                LATITUDE:;-15,78944444
                LONGITUDE:;-47,92583332
                ALTITUDE:;1160,96
                DATA DE FUNDACAO:;07/05/00
                Data;Hora UTC;PRECIPITAÇÃO TOTAL, HORÁRIO (mm);TEMPERATURA DO AR - BULBO SECO, HORARIA (°C);TEMPERATURA MÁXIMA NA HORA ANT. (AUT) (°C);TEMPERATURA MÍNIMA NA HORA ANT. (AUT) (°C);UMIDADE RELATIVA DO AR, HORARIA (%);
                """);
        for (int h = 0; h < 24; h++) {
            // horas UTC 03..23 do dia 05 e 00..02 do dia 06 = dia 05 inteiro em Brasília
            String data = h < 21 ? "2024/01/05" : "2024/01/06";
            int horaUtc = (h + 3) % 24;
            csv.append(data).append(';').append(String.format("%02d00 UTC", horaUtc))
                    .append(";0;").append(h == 5 ? "-9999" : "22,4").append(";23;21,5;80;\n");
        }
        CsvFonteClima fonte = new CsvFonteClima(csv.toString(), SP);

        assertThat(fonte.estacaoDoArquivo()).isEqualTo("A001");
        List<ClimaDiario> dias = fonte.buscar("A001", null, null);
        assertThat(dias).hasSize(1);
        assertThat(dias.get(0).data()).isEqualTo(LocalDate.of(2024, 1, 5));
        assertThat(dias.get(0).tempMedia()).isEqualTo(22.4); // a hora com -9999 fica de fora
        assertThat(dias.get(0).tempMax()).isEqualTo(23.0);
        assertThat(dias.get(0).fonte()).isEqualTo("csv");
    }

    @Test
    void leCsvDiarioSimples() throws Exception {
        CsvFonteClima fonte = new CsvFonteClima("""
                data;temp_media;temp_max;temp_min;umidade_media
                2024-02-01;30;34;24;70
                02/02/2024;25,5;;;65
                """, SP);
        List<ClimaDiario> dias = fonte.buscar("A001", null, null);
        assertThat(dias).hasSize(2);
        assertThat(dias.get(0).ituMedio()).isEqualTo(81.38);
        assertThat(dias.get(0).horasItuAcima72()).isNull();
        assertThat(dias.get(1).tempMax()).isNull();
    }

    @Test
    void leRespostaJsonDoInmetEEscondeOToken() throws Exception {
        InmetFonteClima inmet = new InmetFonteClima("https://exemplo", "SEGREDO123", 5,
                java.time.Clock.system(SP));
        var leituras = inmet.lerResposta("""
                [{"DT_MEDICAO":"2024-01-05","HR_MEDICAO":"1500","TEM_INS":"28.1","UMD_INS":"55","TEM_MAX":"28.9","TEM_MIN":null},
                 {"DT_MEDICAO":"2024-01-05","HR_MEDICAO":"1600","TEM_INS":null,"UMD_INS":"50"}]
                """);
        assertThat(leituras).hasSize(2);
        assertThat(leituras.get(0).instante()).isEqualTo(Instant.parse("2024-01-05T15:00:00Z"));
        assertThat(leituras.get(0).temperatura()).isEqualTo(28.1);
        assertThat(leituras.get(1).temperatura()).isNull();
        assertThat(inmet.lerResposta("")).isEmpty();
        assertThat(inmet.semToken("I/O error on GET https://exemplo/token/x/SEGREDO123")).doesNotContain("SEGREDO123");
    }
}
