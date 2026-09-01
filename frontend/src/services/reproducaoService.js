import { api, FAZENDA_ID } from "./api";

export const reproducaoService = {
  listarPorFazenda: () => api.get(`/fazendas/${FAZENDA_ID()}/reproducao`),
  criar: (animalId, dados) => api.post(`/animais/${animalId}/reproducao`, dados),
  confirmarCio: (eventoId) => api.put(`/reproducao/${eventoId}/confirmar-cio`, {}),
  remover: (id) => api.del(`/reproducao/${id}`),
};
