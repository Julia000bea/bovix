import { api, FAZENDA_ID } from "./api";

export const saudeService = {
  listarPorFazenda: () => api.get(`/fazendas/${FAZENDA_ID()}/saude`),
  criar: (animalId, dados) => api.post(`/animais/${animalId}/saude`, dados),
  remover: (id) => api.del(`/saude/${id}`),
};
