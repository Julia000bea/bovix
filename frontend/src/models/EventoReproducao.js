import { todayStr, addDays } from "../utils/date";

export function novoEventoReproducao() {
  return {
    animalId: "",
    tipo: "cio_detectado",
    data: todayStr(),
    detalhe: "",
    resultado: "",
    cioVinculadoId: null,
    previsaoParto: null,
  };
}
