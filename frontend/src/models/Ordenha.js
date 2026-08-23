import { todayStr } from "../utils/date";

export function novaOrdenha() {
  return { animalId: "", data: todayStr(), periodo: "Dia todo", litros: "" };
}
