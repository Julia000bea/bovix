import { todayStr } from "../utils/date";
import { CATEGORIAS_GASTO } from "../utils/tokens";

export function novoGasto() {
  return { tipo: "despesa", data: todayStr(), categoria: CATEGORIAS_GASTO[0], descricao: "", valor: "" };
}
