import { api, FAZENDA_ID } from "./api";

export const saudeService = {
  listarPorFazenda: () => api.get(`/fazendas/${FAZENDA_ID()}/saude`),
  criar: (animalId, dados) => api.post(`/animais/${animalId}/saude`, dados),
  criarParaRebanho: (dados) => api.post(`/fazendas/${FAZENDA_ID()}/saude/rebanho`, dados),
  remover: (id) => api.del(`/saude/${id}`),
};
