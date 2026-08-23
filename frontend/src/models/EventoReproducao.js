import { todayStr } from "../utils/date";

export function novoEventoReproducao() {
  return { animalId: "", tipo: "cio", data: todayStr(), detalhe: "", resultado: "positivo" };
}
