import { useMemo } from "react";
import { addDays, daysBetween, todayStr } from "../utils/date";
import { CATEGORIAS_GASTO } from "../utils/tokens";
import { litrosPorDia } from "../utils/producao";

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
    const cioProximo = alerts.filter((al) => al.icone === "cio" && al.dias >= 0 && al.dias <= 2);
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

    // 3) produção: últimos 7 dias COMPLETOS (ontem para trás) vs os 7 anteriores.
    // Hoje fica de fora: a ordenha da tarde ainda não foi lançada e geraria "queda" falsa toda manhã.
    // Só compara se as duas semanas têm lançamento em pelo menos 5 dias — dia sem registro não é dia sem leite.
    const producaoPorDia = litrosPorDia(ordenha);
    const semana = (inicioOffset, fimOffset) => {
      let soma = 0;
      let diasComRegistro = 0;
      for (let i = inicioOffset; i <= fimOffset; i++) {
        const litros = producaoPorDia[addDays(today, -i)];
        if (litros !== undefined) {
          soma += litros;
          diasComRegistro += 1;
        }
      }
      // média diária, para comparar semanas com números diferentes de dias lançados
      return { media: diasComRegistro > 0 ? soma / diasComRegistro : 0, diasComRegistro };
    };
    const semanaAtual = semana(1, 7);
    const semanaAnterior = semana(8, 14);
    if (semanaAtual.diasComRegistro >= 5 && semanaAnterior.diasComRegistro >= 5 && semanaAnterior.media > 0) {
      const pct = ((semanaAtual.media - semanaAnterior.media) / semanaAnterior.media) * 100;
      const detalhe = `(média de ${semanaAtual.media.toFixed(0)} L/dia vs ${semanaAnterior.media.toFixed(0)} L/dia)`;
      if (pct <= -5) {
        insights.push({
          id: "ins-producao-queda", tipo: "alerta", icone: "queda",
          texto: `A produção caiu ${Math.abs(pct).toFixed(0)}% nos últimos 7 dias em relação à semana anterior ${detalhe}. Vale checar alimentação, água e saúde do rebanho.`,
        });
      } else if (pct >= 5) {
        insights.push({
          id: "ins-producao-alta", tipo: "positivo", icone: "alta",
          texto: `A produção subiu ${pct.toFixed(0)}% nos últimos 7 dias em relação à semana anterior ${detalhe}.`,
        });
      }
    }

    // 4) custos: do dia 1 até hoje neste mês vs o MESMO trecho do mês anterior, por categoria.
    // Comparar o mês em andamento com o mês anterior inteiro dizia "o custo caiu 70%" todo começo de mês.
    const diaDoMes = Number(today.slice(8, 10));
    const mesAtual = today.slice(0, 7);
    const ultimoDiaMesAnterior = addDays(mesAtual + "-01", -1);
    const mesAnterior = ultimoDiaMesAnterior.slice(0, 7);
    const limiteDiaAnterior = Math.min(diaDoMes, Number(ultimoDiaMesAnterior.slice(8, 10)));
    const totalCategoria = (mes, ateDia, categoria) =>
      financeiro.filter((g) => g.data.slice(0, 7) === mes && Number(g.data.slice(8, 10)) <= ateDia && g.categoria === categoria)
        .reduce((s, g) => s + Number(g.valor || 0), 0);

    if (diaDoMes >= 7) { // na primeira semana do mês ainda há poucos lançamentos para comparar
      CATEGORIAS_GASTO.map((cat) => {
        const atual = totalCategoria(mesAtual, diaDoMes, cat);
        const anterior = totalCategoria(mesAnterior, limiteDiaAnterior, cat);
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
            texto: `O custo com ${c.cat.toLowerCase()} ${c.pct > 0 ? "aumentou" : "caiu"} ${Math.abs(c.pct).toFixed(0)}% de 1 a ${diaDoMes} deste mês, comparado ao mesmo período do mês anterior.`,
          });
        });
    }

    // 5) gestação fora do previsto — possível erro de registro ou atraso real
    const ativos = animais.filter((a) => a.status === "Ativa");
    for (const a of ativos) {
      const repro = reproducao.filter((e) => e.animalId === a.id).sort((x, y) => (x.data < y.data ? 1 : -1));
      // cobertura = inseminação ou monta natural; parto, novo cio ou diagnóstico "vazia" encerram a gestação
      const ultimaCobertura = repro.find((e) => ["inseminacao", "reproducao"].includes(e.tipo));
      if (!ultimaCobertura) continue;
      const temEventoDepois = repro.some(
        (e) => (e.tipo === "parto" || ["cio_detectado", "cio_confirmado"].includes(e.tipo)
          || (e.tipo === "diagnostico_prenhez" && e.resultado === "vazia")) && e.data > ultimaCobertura.data
      );
      if (temEventoDepois) continue;
      const diasGestante = daysBetween(ultimaCobertura.data, today);
      if (diasGestante > gestacaoDias + 10) {
        insights.push({
          id: `ins-gestacao-${a.id}`, tipo: "atencao", icone: "dados",
          texto: `A vaca #${a.brinco}${a.nome ? " (" + a.nome + ")" : ""} está há ${diasGestante} dias desde a última cobertura, ${diasGestante - gestacaoDias} a mais que o previsto. Verifique se o parto já ocorreu e não foi registrado, ou se a data está correta.`,
        });
      }
    }

    return insights;
  }, [animais, reproducao, ordenha, financeiro, config, alerts]);
}
