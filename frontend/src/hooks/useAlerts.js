import { useMemo } from "react";
import { addDays, daysBetween, formatDate, todayStr } from "../utils/date";

/**
 * Motor de alertas do Quadro do dia: cio previsto, parto previsto,
 * carência do leite e reaplicação de vacinas/medicamentos.
 */
export function useAlerts({ animais, reproducao, saude, config }) {
  return useMemo(() => {
    const today = todayStr();
    const alerts = [];
    const ativos = animais.filter((a) => a.status === "Ativa");
    const gestacaoDias = Number(config?.gestacaoDias) || 283;
    const cioDias = Number(config?.cioDias) || 21;

    for (const a of ativos) {
      const repro = reproducao
        .filter((e) => e.animalId === a.id)
        .sort((x, y) => (x.data < y.data ? 1 : -1));
      const last = repro[0];

      if (last?.tipo === "inseminacao") {
        const negDepois = repro.some(
          (e) => e.tipo === "diagnostico" && e.resultado === "negativo" && e.data > last.data
        );
        const partoDepois = repro.some((e) => e.tipo === "parto" && e.data > last.data);

        if (!negDepois && !partoDepois) {
          const previsao = addDays(last.data, gestacaoDias);
          const dias = daysBetween(today, previsao);
          if (dias <= 15 && dias >= -10) {
            alerts.push({
              id: `parto-${a.id}`, animal: a, dias,
              urgencia: dias < 0 ? "atrasado" : dias <= 7 ? "urgente" : "proximo",
              icone: "parto",
              texto: dias < 0
                ? `Parto previsto há ${-dias} dia(s) — ${formatDate(previsao)}`
                : dias === 0
                ? `Parto previsto para hoje`
                : `Parto previsto em ${dias} dia(s) — ${formatDate(previsao)}`,
            });
          }
        } else if (negDepois) {
          const cioBase = repro.find((e) => e.tipo === "diagnostico" && e.resultado === "negativo");
          const proximoCio = addDays(cioBase.data, cioDias);
          const dias = daysBetween(today, proximoCio);
          if (dias >= -3 && dias <= 3) {
            alerts.push({
              id: `retorno-${a.id}`, animal: a, dias,
              urgencia: dias <= 0 ? "urgente" : "proximo",
              icone: "cio",
              texto: `Possível retorno de cio (diagnóstico negativo) — janela ${formatDate(proximoCio)}`,
            });
          }
        }
      }

      if (last?.tipo === "cio") {
        const proximoCio = addDays(last.data, cioDias);
        const dias = daysBetween(today, proximoCio);
        if (dias >= -3 && dias <= 3) {
          alerts.push({
            id: `cio-${a.id}`, animal: a, dias,
            urgencia: dias <= 0 ? "urgente" : "proximo",
            icone: "cio",
            texto: `Janela de cio prevista — ${formatDate(proximoCio)}`,
          });
        }
      }

      const saudeAnimal = saude.filter((e) => e.animalId === a.id);
      for (const e of saudeAnimal) {
        if (e.carenciaLeiteDias) {
          const fim = addDays(e.data, Number(e.carenciaLeiteDias));
          if (today <= fim) {
            alerts.push({
              id: `carencia-${e.id}`, animal: a,
              urgencia: "ativo", icone: "carencia",
              texto: `Leite em carência até ${formatDate(fim)} (${e.produto})`,
            });
          }
        }
        if (e.proximaAplicacaoDias) {
          const prox = addDays(e.data, Number(e.proximaAplicacaoDias));
          const dias = daysBetween(today, prox);
          if (dias <= 15) {
            alerts.push({
              id: `reaplicacao-${e.id}`, animal: a,
              urgencia: dias < 0 ? "atrasado" : dias <= 3 ? "urgente" : "proximo",
              icone: "saude",
              texto: dias < 0
                ? `${e.produto}: reaplicação atrasada (${-dias}d)`
                : `${e.produto}: próxima aplicação em ${dias} dia(s)`,
            });
          }
        }
      }
    }

    const ordem = { atrasado: 0, urgente: 1, ativo: 2, proximo: 3 };
    return alerts.sort((a, b) => ordem[a.urgencia] - ordem[b.urgencia]);
  }, [animais, reproducao, saude, config]);
}
