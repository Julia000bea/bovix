import { api, FAZENDA_ID } from "./api";

export const ordenhaService = {
  listarPorPeriodo: (inicio, fim) => api.get(`/fazendas/${FAZENDA_ID()}/ordenhas?inicio=${inicio}&fim=${fim}`),
  criar: (dados, animalId) => api.post(`/fazendas/${FAZENDA_ID()}/ordenhas${animalId ? `?animalId=${animalId}` : ""}`, dados),
  remover: (id) => api.del(`/ordenhas/${id}`),
};
