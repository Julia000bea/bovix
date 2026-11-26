package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Indicadores de eficiência reprodutiva numa janela de 12 meses terminando em D, sempre com o
 * número de casos usados. Funções puras (sem banco nem relógio).
 *
 * Desfecho de cada cobertura (inseminação ou monta), olhando os eventos seguintes do animal até a
 * próxima cobertura:
 * - CONCEBEU: diagnóstico prenhe, prenhez confirmada, perda gestacional (ela estava prenhe) ou
 *   parto pelo menos {@link #GESTACAO_MINIMA} dias depois;
 * - FALHOU: diagnóstico vazia, novo cio ou nova cobertura antes de qualquer confirmação;
 * - sem desfecho (ainda aguardando): fica fora da taxa de concepção.
 */
public final class IndicadoresReprodutivos {

    /** Parto antes disso, contado da cobertura, não é desta cobertura (registro inconsistente). */
    static final int GESTACAO_MINIMA = 240;
    /** Intervalos entre partos menores que isso são registro duplicado/inconsistente e ficam fora. */
    static final int IEP_MINIMO = 240;

    public enum Desfecho { CONCEBEU, FALHOU, PENDENTE }

    /** Cobertura com o desfecho, a data da confirmação (se houve) e o parto anterior a ela. */
    public record Cobertura(EventoReproducao evento, Desfecho desfecho, LocalDate confirmadaEm, LocalDate partoAnterior) {}

    /** Valor do indicador e quantos casos entraram no cálculo. valor = null se não houve casos. */
    public record Indicador(Double valor, int casos) {}

    public record IepVaca(Long animalId, String brinco, String nome, Double mediaDias, int intervalos) {}

    public record VacaVazia(Long animalId, String brinco, String nome, long diasPosParto) {}

    public record Perdas(int quantidade, int prenhezesConfirmadas, Double percentual) {}

    public record Resultado(LocalDate inicio, LocalDate fim, Indicador iepMedio, List<IepVaca> iepPorVaca,
                            Indicador diasEmAberto, Indicador taxaConcepcao, Perdas perdasGestacionais,
                            int diasVaziaAlerta, List<VacaVazia> vacasVazias) {}

    private static final Comparator<EventoReproducao> CRONOLOGICO =
            Comparator.comparing(EventoReproducao::getData)
                    .thenComparing(EventoReproducao::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private IndicadoresReprodutivos() {}

    /** Primeiro dia da janela de 12 meses que termina em d. */
    public static LocalDate inicioJanela(LocalDate d) {
        return d.minusYears(1).plusDays(1);
    }

    public static Resultado calcular(Collection<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal,
                                     LocalDate d, int gestacaoDias, int diasVaziaAlerta) {
        LocalDate inicio = inicioJanela(d);
        List<IepVaca> iepPorVaca = new ArrayList<>();
        List<Long> todosIntervalos = new ArrayList<>();
        List<Long> diasAbertos = new ArrayList<>();
        int concebeu = 0;
        int falhou = 0;
        int perdas = 0;
        int confirmadas = 0;

        for (Animal a : animais) {
            List<EventoReproducao> eventos = eventosPorAnimal.getOrDefault(a.getId(), List.of());

            List<Long> intervalos = intervalosEntrePartos(eventos, inicio, d);
            if (!intervalos.isEmpty()) {
                todosIntervalos.addAll(intervalos);
                iepPorVaca.add(new IepVaca(a.getId(), a.getBrinco(), a.getNome(), media(intervalos), intervalos.size()));
            }

            for (Cobertura c : coberturas(eventos, d)) {
                LocalDate data = c.evento().getData();
                boolean naJanela = !data.isBefore(inicio);
                if (naJanela && c.desfecho() == Desfecho.CONCEBEU) concebeu++;
                if (naJanela && c.desfecho() == Desfecho.FALHOU) falhou++;
                if (naJanela && c.desfecho() == Desfecho.CONCEBEU && c.partoAnterior() != null) {
                    diasAbertos.add(ChronoUnit.DAYS.between(c.partoAnterior(), data));
                }
                if (c.confirmadaEm() != null && !c.confirmadaEm().isBefore(inicio)) confirmadas++;
            }
            perdas += contarPerdas(eventos, inicio, d);
        }

        int comDesfecho = concebeu + falhou;
        Indicador taxa = new Indicador(comDesfecho == 0 ? null : IndicadoresRebanho.arredondar(100.0 * concebeu / comDesfecho, 1),
                comDesfecho);
        Double percentualPerdas = confirmadas == 0 ? null : IndicadoresRebanho.arredondar(100.0 * perdas / confirmadas, 1);

        iepPorVaca.sort(Comparator.comparing(IepVaca::brinco, Comparator.nullsLast(Comparator.naturalOrder())));
        return new Resultado(inicio, d,
                new Indicador(media(todosIntervalos), todosIntervalos.size()), iepPorVaca,
                new Indicador(media(diasAbertos), diasAbertos.size()),
                taxa,
                new Perdas(perdas, confirmadas, percentualPerdas),
                diasVaziaAlerta, vacasVazias(animais, eventosPorAnimal, d, gestacaoDias, diasVaziaAlerta));
    }

    /** Intervalos (dias) entre partos consecutivos cujo parto mais recente cai em [inicio, fim]. */
    public static List<Long> intervalosEntrePartos(List<EventoReproducao> eventos, LocalDate inicio, LocalDate fim) {
        List<LocalDate> partos = eventos.stream()
                .filter(e -> "parto".equals(e.getTipo()) && !e.getData().isAfter(fim))
                .map(EventoReproducao::getData)
                .sorted()
                .toList();
        List<Long> intervalos = new ArrayList<>();
        for (int i = 1; i < partos.size(); i++) {
            if (partos.get(i).isBefore(inicio)) continue;
            long dias = ChronoUnit.DAYS.between(partos.get(i - 1), partos.get(i));
            if (dias >= IEP_MINIMO) intervalos.add(dias);
        }
        return intervalos;
    }

    /** Média do IEP do rebanho para a janela de 12 meses terminando em d (usada na exportação diária). */
    public static Indicador iepMedio(Collection<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal, LocalDate d) {
        LocalDate inicio = inicioJanela(d);
        List<Long> todos = new ArrayList<>();
        for (Animal a : animais) {
            todos.addAll(intervalosEntrePartos(eventosPorAnimal.getOrDefault(a.getId(), List.of()), inicio, d));
        }
        return new Indicador(media(todos), todos.size());
    }

    /** Todas as coberturas do animal até d, com o desfecho conhecido até d. */
    public static List<Cobertura> coberturas(List<EventoReproducao> eventos, LocalDate d) {
        List<EventoReproducao> ordenados = eventos.stream()
                .filter(e -> !e.getData().isAfter(d))
                .sorted(CRONOLOGICO)
                .toList();
        List<Cobertura> resultado = new ArrayList<>();
        LocalDate ultimoParto = null;
        for (int i = 0; i < ordenados.size(); i++) {
            EventoReproducao e = ordenados.get(i);
            if ("parto".equals(e.getTipo())) {
                ultimoParto = e.getData();
                continue;
            }
            if (!CicloReprodutivo.TIPOS_COBERTURA.contains(e.getTipo())) continue;

            Desfecho desfecho = Desfecho.PENDENTE;
            LocalDate confirmadaEm = null;
            for (int j = i + 1; j < ordenados.size(); j++) {
                EventoReproducao depois = ordenados.get(j);
                String tipo = depois.getTipo();
                boolean confirma = "prenhez_confirmada".equals(tipo)
                        || ("diagnostico_prenhez".equals(tipo) && "prenhe".equals(depois.getResultado()));
                if (confirma) {
                    desfecho = Desfecho.CONCEBEU;
                    if (confirmadaEm == null) confirmadaEm = depois.getData();
                    continue; // a gestação segue; olha adiante só para achar o fim dela
                }
                if (desfecho == Desfecho.CONCEBEU) {
                    if ("parto".equals(tipo) || "perda_gestacional".equals(tipo)
                            || CicloReprodutivo.TIPOS_COBERTURA.contains(tipo)) break;
                    continue;
                }
                if ("perda_gestacional".equals(tipo)) {
                    desfecho = Desfecho.CONCEBEU; // perdeu, mas tinha concebido
                    break;
                }
                if ("parto".equals(tipo)) {
                    if (ChronoUnit.DAYS.between(e.getData(), depois.getData()) >= GESTACAO_MINIMA) desfecho = Desfecho.CONCEBEU;
                    break;
                }
                if (("diagnostico_prenhez".equals(tipo) && "vazia".equals(depois.getResultado()))
                        || CicloReprodutivo.TIPOS_CIO.contains(tipo)
                        || CicloReprodutivo.TIPOS_COBERTURA.contains(tipo)) {
                    desfecho = Desfecho.FALHOU;
                    break;
                }
            }
            resultado.add(new Cobertura(e, desfecho, confirmadaEm, ultimoParto));
        }
        return resultado;
    }

    /** Perdas gestacionais registradas em [inicio, fim]. */
    public static int contarPerdas(List<EventoReproducao> eventos, LocalDate inicio, LocalDate fim) {
        return (int) eventos.stream()
                .filter(e -> "perda_gestacional".equals(e.getTipo())
                        && !e.getData().isBefore(inicio) && !e.getData().isAfter(fim))
                .count();
    }

    /**
     * Vacas no rebanho em d que pariram há mais de X dias e não estão prenhes nem cobertas aguardando
     * diagnóstico (situação sem cobertura, em cio ou vazia).
     */
    public static List<VacaVazia> vacasVazias(Collection<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal,
                                              LocalDate d, int gestacaoDias, int diasVaziaAlerta) {
        List<VacaVazia> vazias = new ArrayList<>();
        for (Animal a : animais) {
            if (!IndicadoresRebanho.presente(a, d)) continue;
            CicloReprodutivo.Estado estado = CicloReprodutivo.estado(
                    eventosPorAnimal.getOrDefault(a.getId(), List.of()), d, gestacaoDias);
            if (estado.ultimoParto() == null) continue;
            boolean vazia = switch (estado.situacao()) {
                case SEM_COBERTURA, CIO, VAZIA -> true;
                case COBERTA, PRENHE -> false;
            };
            long dias = ChronoUnit.DAYS.between(estado.ultimoParto(), d);
            if (vazia && dias > diasVaziaAlerta) {
                vazias.add(new VacaVazia(a.getId(), a.getBrinco(), a.getNome(), dias));
            }
        }
        vazias.sort(Comparator.comparingLong(VacaVazia::diasPosParto).reversed());
        return vazias;
    }

    static Double media(List<Long> valores) {
        if (valores.isEmpty()) return null;
        return IndicadoresRebanho.arredondar(valores.stream().mapToLong(Long::longValue).average().orElse(0), 1);
    }
}
