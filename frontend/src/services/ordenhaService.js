import { api, FAZENDA_ID } from "./api";
import { descrever, ordenhasProvisorias } from "./provisorio";

export const ordenhaService = {
  listarPorPeriodo: (inicio, fim, opcoes) => api.get(`/fazendas/${FAZENDA_ID()}/ordenhas?inicio=${inicio}&fim=${fim}`, opcoes),
  // sem internet, fica guardada no aparelho e é enviada quando a conexão voltar
  criar: (dados, animalId) =>
    api.post(`/fazendas/${FAZENDA_ID()}/ordenhas${animalId ? `?animalId=${animalId}` : ""}`, dados, {
      colecao: "ordenha",
      descricao: descrever.ordenha(dados),
      provisorio: (localId) => ordenhasProvisorias(localId, dados, animalId),
    }),
  // CSV "data;litros" com a produção diária do rebanho; devolve { importadas, substituidas, rejeitadas }
  importar: (arquivo) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    return api.postForm(`/fazendas/${FAZENDA_ID()}/ordenhas/importar`, fd);
  },
  remover: (id) => api.del(`/ordenhas/${id}`),
};
