// Produção de leite por dia sem contar em dobro quando há lançamentos de "total do rebanho"
// e por animal no mesmo dia:
// - um total de "Dia todo" vale para o dia inteiro;
// - senão, em cada período (Manhã/Tarde), o total do rebanho vale; sem ele, somam-se os animais.
export function litrosPorDia(ordenha) {
  const dias = {};
  for (const o of ordenha) {
    const dia = (dias[o.data] ||= { totalDiaTodo: null, periodos: {} });
    const litros = Number(o.litros || 0);
    if (o.animalId == null && o.periodo === "Dia todo") {
      dia.totalDiaTodo = (dia.totalDiaTodo || 0) + litros;
      continue;
    }
    const p = (dia.periodos[o.periodo] ||= { total: null, individual: 0 });
    if (o.animalId == null) p.total = (p.total || 0) + litros;
    else p.individual += litros;
  }

  const resultado = {};
  for (const [data, dia] of Object.entries(dias)) {
    resultado[data] = dia.totalDiaTodo !== null
      ? dia.totalDiaTodo
      : Object.values(dia.periodos).reduce((s, p) => s + (p.total !== null ? p.total : p.individual), 0);
  }
  return resultado;
}
