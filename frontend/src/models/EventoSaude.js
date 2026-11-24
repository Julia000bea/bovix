import { todayStr } from "../utils/date";

export function novoEventoSaude() {
  return {
    animalId: "",
    tipo: "vacina",
    produto: "",
    data: todayStr(),
    duracaoTratamentoDias: "",
    carenciaLeiteDias: "",
    proximaAplicacaoDias: "",
    observacoes: "",
  };
}
