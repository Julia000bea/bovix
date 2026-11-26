package com.bovix.service;

import com.bovix.calculo.IndicadoresRebanho;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Série diária de produção e lactação do rebanho (tarefa 2 do TCC). */
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
    private final FazendaService fazendaService;

    @Transactional(readOnly = true)
    public List<PontoSerie> serie(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        validarPeriodo(inicio, fim);
        return calcularSerie(carregar(fazendaId), producaoRepository.totaisPorDia(fazendaId, inicio, fim), inicio, fim);
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
