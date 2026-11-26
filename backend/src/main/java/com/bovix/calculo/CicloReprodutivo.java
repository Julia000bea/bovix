package com.bovix.calculo;

import com.bovix.model.EventoReproducao;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Situação reprodutiva de um animal numa data D. É a única implementação da regra, usada pelo
 * Quadro do dia (PainelDoDiaService), pelos indicadores reprodutivos e pela projeção de lactação.
 *
 * Regra: depois do último parto, o evento de cio ou cobertura (inseminação/monta) mais recente
 * define a fase. Se for uma cobertura, os eventos posteriores dizem se a gestação segue:
 * o diagnóstico/confirmação mais recente decide; diagnóstico "vazia" ou perda gestacional
 * encerram a gestação (a vaca volta a ser vazia).
 */
public final class CicloReprodutivo {

    public static final Set<String> TIPOS_COBERTURA = Set.of("inseminacao", "reproducao");
    public static final Set<String> TIPOS_CIO = Set.of("cio_detectado", "cio_confirmado");

    /** Mais recente primeiro; no mesmo dia, o registrado por último primeiro. */
    public static final Comparator<EventoReproducao> MAIS_RECENTE_PRIMEIRO =
            Comparator.comparing(EventoReproducao::getData)
                    .thenComparing(EventoReproducao::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .reversed();

    public enum Situacao {
        /** Sem cio nem cobertura desde o último parto. */
        SEM_COBERTURA,
        /** Último marco é um cio. */
        CIO,
        /** Coberta, ainda sem diagnóstico. */
        COBERTA,
        /** Gestação confirmada (diagnóstico prenhe ou prenhez confirmada). */
        PRENHE,
        /** Diagnóstico vazia ou perda gestacional depois da cobertura. */
        VAZIA
    }

    /**
     * @param ultimoParto   último parto até D (null se nunca pariu)
     * @param marco         cio ou cobertura mais recente depois do último parto (null em SEM_COBERTURA)
     * @param cobertura     a cobertura vigente, quando o marco é uma cobertura
     * @param posCobertura  eventos depois da cobertura, mais recente primeiro
     * @param encerramento  diagnóstico vazia ou perda gestacional que encerrou a gestação (VAZIA)
     * @param previsaoParto previsão informada num diagnóstico/confirmação ou cobertura + gestação
     * @param secou         houve secagem ou pré-parto depois da cobertura
     * @param entrouPreParto houve pré-parto depois da cobertura
     */
    public record Estado(LocalDate ultimoParto, Situacao situacao, EventoReproducao marco, EventoReproducao cobertura,
                         List<EventoReproducao> posCobertura, EventoReproducao encerramento, LocalDate previsaoParto,
                         boolean secou, boolean entrouPreParto) {}

    private CicloReprodutivo() {}

    /** @param eventos eventos do animal, em qualquer ordem; os posteriores a D são ignorados */
    public static Estado estado(List<EventoReproducao> eventos, LocalDate d, int gestacaoDias) {
        List<EventoReproducao> ordenados = eventos.stream()
                .filter(e -> !e.getData().isAfter(d))
                .sorted(MAIS_RECENTE_PRIMEIRO)
                .toList();

        LocalDate ultimoParto = ordenados.stream()
                .filter(e -> "parto".equals(e.getTipo()))
                .map(EventoReproducao::getData)
                .findFirst().orElse(null);

        List<EventoReproducao> ciclo = ordenados.stream()
                .filter(e -> ultimoParto == null || e.getData().isAfter(ultimoParto))
                .toList();

        EventoReproducao marco = ciclo.stream()
                .filter(e -> TIPOS_CIO.contains(e.getTipo()) || TIPOS_COBERTURA.contains(e.getTipo()))
                .findFirst().orElse(null);
        if (marco == null) {
            return new Estado(ultimoParto, Situacao.SEM_COBERTURA, null, null, List.of(), null, null, false, false);
        }
        if (TIPOS_CIO.contains(marco.getTipo())) {
            return new Estado(ultimoParto, Situacao.CIO, marco, null, List.of(), null, null, false, false);
        }

        List<EventoReproducao> posCobertura = ciclo.subList(0, ciclo.indexOf(marco));

        // o diagnóstico/confirmação/perda mais recente decide se a gestação segue
        EventoReproducao ultimoResultado = posCobertura.stream()
                .filter(e -> "prenhez_confirmada".equals(e.getTipo())
                        || "perda_gestacional".equals(e.getTipo())
                        || ("diagnostico_prenhez".equals(e.getTipo()) && e.getResultado() != null))
                .findFirst().orElse(null);

        Situacao situacao;
        EventoReproducao encerramento = null;
        if (ultimoResultado == null) {
            situacao = Situacao.COBERTA;
        } else if ("perda_gestacional".equals(ultimoResultado.getTipo()) || "vazia".equals(ultimoResultado.getResultado())) {
            situacao = Situacao.VAZIA;
            encerramento = ultimoResultado;
        } else {
            situacao = Situacao.PRENHE;
        }

        LocalDate previsao = posCobertura.stream()
                .filter(e -> e.getPrevisaoParto() != null)
                .map(EventoReproducao::getPrevisaoParto)
                .findFirst()
                .orElse(marco.getData().plusDays(gestacaoDias));
        boolean secou = posCobertura.stream().anyMatch(e -> "secagem".equals(e.getTipo()) || "pre_parto".equals(e.getTipo()));
        boolean preParto = posCobertura.stream().anyMatch(e -> "pre_parto".equals(e.getTipo()));

        return new Estado(ultimoParto, situacao, marco, marco, posCobertura, encerramento, previsao, secou, preParto);
    }
}
