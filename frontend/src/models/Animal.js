import { CATEGORIAS } from "../utils/tokens";

/** Fábrica de um Animal em branco para alimentar formulários. */
export function novoAnimal() {
  return {
    brinco: "",
    nome: "",
    raca: "",
    nascimento: "",
    categoria: CATEGORIAS[0],
    status: "Ativa",
    notas: "",
    fotoUrl: "",
  };
}

export function rotuloAnimal(animal) {
  if (!animal) return "animal removido";
  return `#${animal.brinco} · ${animal.nome || "sem nome"}`;
}
