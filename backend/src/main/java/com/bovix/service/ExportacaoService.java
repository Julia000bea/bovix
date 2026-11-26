package com.bovix.service;

import com.bovix.calculo.IndicadoresReprodutivos;
import com.bovix.clima.ClimaDiario;
import com.bovix.model.Fazenda;
import com.bovix.model.Ocorrencia;
import com.bovix.repository.ClimaRepository;
import com.bovix.repository.OcorrenciaRepository;
import com.bovix.repository.ProducaoDiariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dataset diário para análise (TCC): uma linha por dia do período.
 * CSV com separador vírgula, ponto decimal e UTF-8. Dia sem dado fica vazio, não zero.
 */
@Service
@RequiredArgsConstructor
public class ExportacaoService {

    public static final String CABECALHO = "data,litros_total,estimado,vacas_em_lactacao,del_medio,"
            + "litros_por_vaca_em_lactacao,itu_medio,itu_max,horas_itu_acima_72,iep_medio_12m,"
            + "vacas_vazias_acima_x,perdas_gestacionais_30d,ocorrencias";

    private final IndicadoresService indicadoresService;
    private final ProducaoDiariaRepository producaoRepository;
    private final ClimaRepository climaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final FazendaService fazendaService;

    @Transactional(readOnly = true)
    public String exportarCsv(Long fazendaId, LocalDate inicio, LocalDate fim) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        IndicadoresService.validarPeriodo(inicio, fim);

        IndicadoresService.DadosFazenda dados = indicadoresService.carregar(fazendaId);
        IndicadoresService.Parametros p = indicadoresService.parametros(fazendaId);
        List<IndicadoresService.PontoSerie> serie = IndicadoresService.calcularSerie(
                dados, producaoRepository.totaisPorDia(fazendaId, inicio, fim), inicio, fim);

        Map<LocalDate, ClimaDiario> clima = fazenda.getEstacaoInmet() == null ? Map.of()
                : climaRepository.serie(fazenda.getEstacaoInmet(), inicio, fim).stream()
                        .collect(Collectors.toMap(ClimaDiario::data, Function.identity()));
        Map<LocalDate, TreeSet<String>> ocorrencias = ocorrenciaRepository
                .findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim).stream()
                .collect(Collectors.groupingBy(Ocorrencia::getData, Collectors.mapping(Ocorrencia::getTipo,
                        Collectors.toCollection(TreeSet::new))));

        StringBuilder csv = new StringBuilder(CABECALHO).append('\n');
        for (IndicadoresService.PontoSerie ponto : serie) {
            LocalDate d = ponto.data();
            ClimaDiario c = clima.get(d);
            IndicadoresReprodutivos.Indicador iep = IndicadoresReprodutivos.iepMedio(dados.animais(), dados.eventosPorAnimal(), d);
            int vazias = IndicadoresReprodutivos.vacasVazias(dados.animais(), dados.eventosPorAnimal(), d,
                    p.gestacaoDias(), p.diasVaziaAlerta()).size();
            int perdas30d = dados.animais().stream()
                    .mapToInt(a -> IndicadoresReprodutivos.contarPerdas(
                            dados.eventosPorAnimal().getOrDefault(a.getId(), List.of()), d.minusDays(29), d))
                    .sum();
            TreeSet<String> tipos = ocorrencias.get(d);

            csv.append(d).append(',')
                    .append(valor(ponto.litrosTotal())).append(',')
                    .append(ponto.estimado() == null ? "" : ponto.estimado() ? "1" : "0").append(',')
                    .append(ponto.vacasEmLactacao()).append(',')
                    .append(valor(ponto.delMedio())).append(',')
                    .append(valor(ponto.litrosPorVacaEmLactacao())).append(',')
                    .append(c == null ? "" : valor(c.ituMedio())).append(',')
                    .append(c == null ? "" : valor(c.ituMax())).append(',')
                    .append(c == null ? "" : valor(c.horasItuAcima72())).append(',')
                    .append(valor(iep.valor())).append(',')
                    .append(vazias).append(',')
                    .append(perdas30d).append(',')
                    .append(tipos == null ? "" : String.join("|", tipos))
                    .append('\n');
        }
        return csv.toString();
    }

    /** Número com ponto decimal; null vira célula vazia. */
    static String valor(Object v) {
        if (v == null) return "";
        if (v instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString();
        if (v instanceof Double x) {
            return x == Math.rint(x) ? String.valueOf(x.longValue()) : String.format(Locale.ROOT, "%s", x);
        }
        return v.toString();
    }
}
