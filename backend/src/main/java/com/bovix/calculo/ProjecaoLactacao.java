package com.bovix.calculo;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Projeção de vacas em lactação para os próximos meses, a partir da situação de hoje.
 * Função pura.
 *
 * - Vaca prenhe em lactação: sai na secagem prevista (parto previsto − período seco) e volta no
 *   parto previsto. Se já foi secada, só volta no parto.
 * - Vaca prenhe seca (ou novilha prenhe): entra em lactação no parto previsto.
 * - Vaca em lactação sem prenhez (sem cobertura, em cio, vazia ou coberta sem diagnóstico): não dá
 *   para prever quando seca; é contada à parte em "semPrevisao", em todos os meses.
 * - Secagem ou parto previstos para antes de hoje (atrasados) são considerados para hoje.
 * - "emLactacao" de cada mês é a situação no último dia do mês (não inclui as "sem previsão").
 */
public final class ProjecaoLactacao {

    public record Mes(YearMonth mes, int emLactacao, int secagens, int partos, int semPrevisao) {}

    private record Agenda(boolean lactandoHoje, LocalDate secagem, LocalDate parto) {
        boolean lactando(LocalDate d) {
            if (parto != null && !d.isBefore(parto)) return true;          // depois do parto: nova lactação
            if (!lactandoHoje) return false;
            return secagem == null || d.isBefore(secagem);                  // até a secagem
        }
    }

    private ProjecaoLactacao() {}

    public static List<Mes> projetar(Collection<Animal> animais, Map<Long, List<EventoReproducao>> eventosPorAnimal,
                                     LocalDate hoje, int gestacaoDias, int periodoSecoDias, int meses) {
        List<Agenda> agendas = new ArrayList<>();
        int semPrevisao = 0;
        for (Animal a : animais) {
            if (!"Ativa".equals(a.getStatus())) continue; // vendida/morta não entra na projeção
            List<EventoReproducao> eventos = eventosPorAnimal.getOrDefault(a.getId(), List.of());
            boolean lactando = IndicadoresRebanho.emLactacao(a, eventos, hoje);
            CicloReprodutivo.Estado estado = CicloReprodutivo.estado(eventos, hoje, gestacaoDias);

            if (estado.situacao() == CicloReprodutivo.Situacao.PRENHE) {
                LocalDate parto = max(estado.previsaoParto(), hoje);
                LocalDate secagem = null;
                if (lactando && !estado.secou()) secagem = max(parto.minusDays(periodoSecoDias), hoje);
                agendas.add(new Agenda(lactando, secagem, parto));
            } else if (lactando) {
                semPrevisao++;
            }
        }

        List<Mes> resultado = new ArrayList<>();
        YearMonth primeiro = YearMonth.from(hoje);
        for (int i = 0; i < meses; i++) {
            YearMonth mes = primeiro.plusMonths(i);
            LocalDate inicio = i == 0 ? hoje : mes.atDay(1);
            LocalDate fim = mes.atEndOfMonth();
            int emLactacao = 0;
            int secagens = 0;
            int partos = 0;
            for (Agenda ag : agendas) {
                if (ag.lactando(fim)) emLactacao++;
                if (ag.secagem() != null && entre(ag.secagem(), inicio, fim)) secagens++;
                if (ag.parto() != null && entre(ag.parto(), inicio, fim)) partos++;
            }
            resultado.add(new Mes(mes, emLactacao, secagens, partos, semPrevisao));
        }
        return resultado;
    }

    private static boolean entre(LocalDate d, LocalDate inicio, LocalDate fim) {
        return !d.isBefore(inicio) && !d.isAfter(fim);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
