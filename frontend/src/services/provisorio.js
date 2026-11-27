// Registros "provisórios": o que a tela mostra logo após um lançamento feito sem internet,
// até a fila ser enviada e os dados reais virem da API (aí eles são substituídos).
import { addDays, formatDate } from "../utils/date";

const numeroOuNull = (v) => (v === "" || v === null || v === undefined ? null : Number(v));

export function provisorio(localId, dados, extras = {}) {
  return { ...dados, ...extras, id: localId, pendente: true };
}

/** Ordenha: com coleta de N dias, o volume é dividido entre os N dias (como faz a API). */
export function ordenhasProvisorias(localId, dados, animalId) {
  const dias = Math.max(1, Number(dados.diasColeta) || 1);
  const litros = Number(dados.litros) || 0;
  const base = { ...dados, animalId: numeroOuNull(animalId), litros, diasColeta: dias, pendente: true };
  if (dias === 1) return [{ ...base, id: localId, estimado: false, coletaId: null }];
  const porDia = Math.round((litros / dias) * 100) / 100;
  return Array.from({ length: dias }, (_, i) => ({
    ...base,
    id: `${localId}:${i}`,
    data: addDays(dados.data, -i),
    litros: porDia,
    estimado: true,
    coletaId: localId,
  }));
}

export const descrever = {
  ordenha: (d) => `Ordenha ${formatDate(d.data)} · ${d.litros} L${Number(d.diasColeta) > 1 ? ` (coleta de ${d.diasColeta} dias)` : ""}`,
  reproducao: (d) => `Reprodução ${formatDate(d.data)} · ${String(d.tipo || "").replaceAll("_", " ")}`,
  saude: (d, rebanho = false) => `Saúde ${formatDate(d.data)} · ${d.produto || d.tipo}${rebanho ? " (todo o rebanho)" : ""}`,
  financeiro: (d) => `Financeiro ${formatDate(d.data)} · ${d.descricao || d.categoria} · R$ ${d.valor}`,
};

export { numeroOuNull };
