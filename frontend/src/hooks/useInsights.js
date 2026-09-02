import { useMemo } from "react";
import { addDays, daysBetween, todayStr } from "../utils/date";
import { CATEGORIAS_GASTO } from "../utils/tokens";

/**
 * Motor de insights de decisão do Dashboard: agrega alertas e cruza tendências
 * (produção semana a semana, custos mês a mês) e aponta possíveis erros de registro.
 */
export function useInsights({ animais, reproducao, ordenha, financeiro, config }, alerts) {
  return useMemo(() => {
    const insights = [];
    const today = todayStr();
    const gestacaoDias = Number(config?.gestacaoDias) || 283;

    // 1) cio previsto — agrupado para os próximos 2 dias
    const cioProximo = alerts.filter((al) => al.icone === "cio" && al.dias <= 2);
    if (cioProximo.length > 0) {
      insights.push({
        id: "ins-cio", tipo: "atencao", icone: "cio",
        texto: `Você tem ${cioProximo.length} vaca${cioProximo.length > 1 ? "s" : ""} com cio previsto para os próximos 2 dias: ${cioProximo
          .map((c) => `#${c.animal.brinco}`).slice(0, 6).join(", ")}${cioProximo.length > 6 ? "…" : ""}.`,
      });
    }

    // 2) parto previsto — agrupado para os próximos 5 dias
    const partoProximo = alerts.filter((al) => al.icone === "parto" && al.dias <= 5 && al.dias >= 0);
    if (partoProximo.length > 0) {
      insights.push({
        id: "ins-parto", tipo: "atencao", icone: "parto",
        texto: `${partoProximo.length} vaca${partoProximo.length > 1 ? "s" : ""} com parto previsto para os próximos 5 dias: ${partoProximo
          .map((c) => `#${c.animal.brinco}`).slice(0, 6).join(", ")}${partoProximo.length > 6 ? "…" : ""}. Fique de olho no curral maternidade.`,
      });
    }

    // 3) produção: últimos 7 dias vs 7 dias anteriores
    const somaPeriodo = (inicioOffset, fimOffset) => {
      let s = 0;
      for (let i = inicioOffset; i <= fimOffset; i++) {
        const d = addDays(today, -i);
        s += ordenha.filter((o) => o.data === d).reduce((acc, o) => acc + Number(o.litros || 0), 0);
      }
      return s;
    };
    const semanaAtual = somaPeriodo(0, 6);
    const semanaAnterior = somaPeriodo(7, 13);
    if (semanaAnterior > 0) {
      const pct = ((semanaAtual - semanaAnterior) / semanaAnterior) * 100;
      if (pct <= -5) {
        insights.push({
          id: "ins-producao-queda", tipo: "alerta", icone: "queda",
          texto: `A produção caiu ${Math.abs(pct).toFixed(0)}% em relação à semana passada (${semanaAtual.toFixed(0)} L vs ${semanaAnterior.toFixed(0)} L). Vale checar alimentação, água e saúde do rebanho.`,
        });
      } else if (pct >= 5) {
        insights.push({
          id: "ins-producao-alta", tipo: "positivo", icone: "alta",
          texto: `A produção subiu ${pct.toFixed(0)}% em relação à semana passada (${semanaAtual.toFixed(0)} L vs ${semanaAnterior.toFixed(0)} L).`,
        });
      }
    }

    // 4) custos: mês atual vs mês anterior, por categoria
    const mesAtual = today.slice(0, 7);
    const mesAnterior = addDays(mesAtual + "-01", -1).slice(0, 7);
    const totalCategoriaMes = (mes, categoria) =>
      financeiro.filter((g) => g.data.slice(0, 7) === mes && g.categoria === categoria)
        .reduce((s, g) => s + Number(g.valor || 0), 0);

    CATEGORIAS_GASTO.map((cat) => {
      const atual = totalCategoriaMes(mesAtual, cat);
      const anterior = totalCategoriaMes(mesAnterior, cat);
      const pct = anterior > 0 ? ((atual - anterior) / anterior) * 100 : null;
      return { cat, pct };
    })
      .filter((c) => c.pct !== null && Math.abs(c.pct) >= 10)
      .sort((a, b) => Math.abs(b.pct) - Math.abs(a.pct))
      .slice(0, 2)
      .forEach((c) => {
        insights.push({
          id: `ins-custo-${c.cat}`,
          tipo: c.pct > 0 ? "alerta" : "positivo",
          icone: c.pct > 0 ? "queda" : "alta",
          texto: `O custo com ${c.cat.toLowerCase()} ${c.pct > 0 ? "aumentou" : "caiu"} ${Math.abs(c.pct).toFixed(0)}% neste mês em relação ao anterior.`,
        });
      });

    // 5) gestação fora do previsto — possível erro de registro ou atraso real
    const ativos = animais.filter((a) => a.status === "Ativa");
    for (const a of ativos) {
      const repro = reproducao.filter((e) => e.animalId === a.id).sort((x, y) => (x.data < y.data ? 1 : -1));
      const ultimaInsem = repro.find((e) => e.tipo === "inseminacao");
      if (!ultimaInsem) continue;
      const temEventoDepois = repro.some(
        (e) => (e.tipo === "parto" || (["diagnostico", "diagnostico_prenhez"].includes(e.tipo)
          && ["negativo", "vazia"].includes(e.resultado))) && e.data > ultimaInsem.data
      );
      if (temEventoDepois) continue;
      const diasGestante = daysBetween(ultimaInsem.data, today);
      if (diasGestante > gestacaoDias + 10) {
        insights.push({
          id: `ins-gestacao-${a.id}`, tipo: "atencao", icone: "dados",
          texto: `A vaca #${a.brinco}${a.nome ? " (" + a.nome + ")" : ""} está há ${diasGestante} dias desde a última inseminação, ${diasGestante - gestacaoDias} a mais que o previsto. Verifique se o parto já ocorreu e não foi registrado, ou se a data está correta.`,
        });
      }
    }

    return insights;
  }, [animais, reproducao, ordenha, financeiro, config, alerts]);
}
