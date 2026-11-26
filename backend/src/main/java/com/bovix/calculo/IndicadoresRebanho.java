package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indicadores do rebanho para uma data D. Funções puras: recebem os animais e os eventos já
 * carregados e não acessam banco nem relógio.
 *
 * Vaca em lactação em D:
 * - teve parto em data ≤ D;
 * - não teve secagem (nem entrada no pré-parto, que no Bovix também marca a vaca como seca)
 *   depois do último parto e até D;
 * - não saiu do rebanho antes de D (data_saida < D).
 */
public final class IndicadoresRebanho {

    /** Eventos que encerram a lactação. "pre_parto" também: o Bovix já trata a vaca como seca nele. */
    static final Set<String> FIM_DA_LACTACAO = Set.of("secagem", "pre_parto");

    public record Lactacao(int vacasEmLactacao, Double delMedio) {}

    private IndicadoresRebanho() {}

    /** Último parto do animal com data ≤ d, ou null. */
    public static LocalDate ultimoParto(List<EventoReproducao> eventos, LocalDate d) {
        LocalDate ultimo = null;
        for (EventoReproducao e : eventos) {
            if ("parto".equals(e.getTipo()) && !e.getData().isAfter(d) && (ultimo == null || e.getData().isAfter(ultimo))) {
                ultimo = e.getData();
            }
        }
        return ultimo;
    }

    /** O animal estava no rebanho em d (não saiu antes de d). */
    public static boolean presente(Animal animal, LocalDate d) {
        return animal.getDataSaida() == null || !animal.getDataSaida().isBefore(d);
    }

    public static boolean emLactacao(Animal animal, List<EventoReproducao> eventos, LocalDate d) {
        if (!presente(animal, d)) return false;
        LocalDate parto = ultimoParto(eventos, d);
        if (parto == null) return false;
        for (EventoReproducao e : eventos) {
            if (FIM_DA_LACTACAO.contains(e.getTipo()) && e.getData().isAfter(parto) && !e.getData().isAfter(d)) {
                return false;
            }
        }
        return true;
    }

    /** Dias em lactação (dias desde o último parto) de uma vaca em lactação em d. */
    public static long del(List<EventoReproducao> eventos, LocalDate d) {
        return ChronoUnit.DAYS.between(ultimoParto(eventos, d), d);
    }

    public static Lactacao lactacao(Collection<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal, LocalDate d) {
        int vacas = 0;
        long somaDel = 0;
        for (Animal a : animais) {
            List<EventoReproducao> eventos = eventosPorAnimal.getOrDefault(a.getId(), List.of());
            if (emLactacao(a, eventos, d)) {
                vacas++;
                somaDel += del(eventos, d);
            }
        }
        Double delMedio = vacas == 0 ? null : arredondar((double) somaDel / vacas, 1);
        return new Lactacao(vacas, delMedio);
    }

    /** Litros por vaca em lactação; null se não há litros ou não há vacas em lactação. */
    public static Double litrosPorVaca(BigDecimal litros, int vacasEmLactacao) {
        if (litros == null || vacasEmLactacao <= 0) return null;
        return litros.divide(BigDecimal.valueOf(vacasEmLactacao), 2, RoundingMode.HALF_UP).doubleValue();
    }

    static Double arredondar(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP).doubleValue();
    }
}
