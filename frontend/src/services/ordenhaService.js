import { api, FAZENDA_ID } from "./api";

export const ordenhaService = {
  listarPorPeriodo: (inicio, fim) => api.get(`/fazendas/${FAZENDA_ID()}/ordenhas?inicio=${inicio}&fim=${fim}`),
  criar: (dados, animalId) => api.post(`/fazendas/${FAZENDA_ID()}/ordenhas${animalId ? `?animalId=${animalId}` : ""}`, dados),
  // CSV "data;litros" com a produção diária do rebanho; devolve { importadas, substituidas, rejeitadas }
  importar: (arquivo) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    return api.postForm(`/fazendas/${FAZENDA_ID()}/ordenhas/importar`, fd);
  },
  remover: (id) => api.del(`/ordenhas/${id}`),
};
