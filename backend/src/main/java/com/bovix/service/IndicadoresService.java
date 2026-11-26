package com.bovix.service;

import com.bovix.calculo.IndicadoresRebanho;
import com.bovix.calculo.IndicadoresReprodutivos;
import com.bovix.calculo.ProjecaoLactacao;
import com.bovix.model.Configuracao;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.repository.ProducaoDiariaRepository;
import com.bovix.repository.ProducaoDiariaRepository.ProducaoDia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indicadores do TCC: série diária do rebanho, indicadores reprodutivos e projeção de lactação. */
@Service
@RequiredArgsConstructor
public class IndicadoresService {

    /** Período máximo de uma consulta (~3 anos). */
    public static final long MAX_DIAS = 1096;

    /**
     * Um dia da série. Campos sem dado ficam null (não zero).
     * misturado = dia antigo com registros por animal e do rebanho; o total usa só os por animal.
     */
    public record PontoSerie(LocalDate data, BigDecimal litrosTotal, Boolean estimado, int vacasEmLactacao,
                             Double delMedio, Double litrosPorVacaEmLactacao, boolean misturado) {}

    /** Dados da fazenda carregados uma vez e reaproveitados nos cálculos de vários dias. */
    public record DadosFazenda(List<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal) {}

    private final AnimalRepository animalRepository;
    private final EventoReproducaoRepository reproducaoRepository;
    private final ProducaoDiariaRepository producaoRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final FazendaService fazendaService;
    private final Clock clock;

    /** Parâmetros da fazenda usados nos cálculos (com os padrões quando não configurados). */
    public record Parametros(int gestacaoDias, int diasVaziaAlerta, int periodoSecoDias) {}

    @Transactional(readOnly = true)
    public List<PontoSerie> serie(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        validarPeriodo(inicio, fim);
        return calcularSerie(carregar(fazendaId), producaoRepository.totaisPorDia(fazendaId, inicio, fim), inicio, fim);
    }

    /** Indicadores reprodutivos dos últimos 12 meses (até hoje). */
    @Transactional(readOnly = true)
    public IndicadoresReprodutivos.Resultado reproducao(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        Parametros p = parametros(fazendaId);
        DadosFazenda dados = carregar(fazendaId);
        return IndicadoresReprodutivos.calcular(dados.animais(), dados.eventosPorAnimal(), LocalDate.now(clock),
                p.gestacaoDias(), p.diasVaziaAlerta());
    }

    /** Projeção de vacas em lactação para os próximos 12 meses. */
    @Transactional(readOnly = true)
    public List<ProjecaoLactacao.Mes> projecao(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        Parametros p = parametros(fazendaId);
        DadosFazenda dados = carregar(fazendaId);
        return ProjecaoLactacao.projetar(dados.animais(), dados.eventosPorAnimal(), LocalDate.now(clock),
                p.gestacaoDias(), p.periodoSecoDias(), 12);
    }

    /** Sem checagem de dono: só para uso interno, depois de verificarPropriedade. */
    Parametros parametros(Long fazendaId) {
        Configuracao c = configuracaoRepository.findByFazenda_Id(fazendaId).orElse(null);
        return new Parametros(
                c != null && c.getGestacaoDias() != null ? c.getGestacaoDias() : 283,
                c != null && c.getDiasVaziaAlerta() != null ? c.getDiasVaziaAlerta() : 90,
                c != null && c.getPeriodoSecoDias() != null ? c.getPeriodoSecoDias() : 60);
    }

    static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial precisa ser anterior à final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAX_DIAS) {
            throw new RegraNegocioException("Período máximo de " + MAX_DIAS + " dias.");
        }
    }

    /** Sem checagem de dono: só para uso interno, depois de verificarPropriedade. */
    DadosFazenda carregar(Long fazendaId) {
        List<Animal> animais = animalRepository.findByFazenda_IdOrderByBrincoAsc(fazendaId);
        Map<Long, List<EventoReproducao>> eventos = reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId)
                .stream().collect(Collectors.groupingBy(EventoReproducao::getAnimalId));
        return new DadosFazenda(animais, eventos);
    }

    static List<PontoSerie> calcularSerie(DadosFazenda dados, List<ProducaoDia> producao, LocalDate inicio, LocalDate fim) {
        Map<LocalDate, ProducaoDia> porDia = producao.stream()
                .collect(Collectors.toMap(ProducaoDia::data, Function.identity()));
        List<PontoSerie> serie = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            IndicadoresRebanho.Lactacao lactacao = IndicadoresRebanho.lactacao(dados.animais(), dados.eventosPorAnimal(), d);
            ProducaoDia p = porDia.get(d);
            BigDecimal litros = p != null ? p.litros() : null;
            serie.add(new PontoSerie(d, litros, p != null ? p.estimado() : null, lactacao.vacasEmLactacao(),
                    lactacao.delMedio(), IndicadoresRebanho.litrosPorVaca(litros, lactacao.vacasEmLactacao()),
                    p != null && p.misturado()));
        }
        return serie;
    }
}
