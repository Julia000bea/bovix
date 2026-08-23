import { api, FAZENDA_ID } from "./api";

export const animaisService = {
  listar: () => api.get(`/fazendas/${FAZENDA_ID()}/animais`),
  criar: (dados) => api.post(`/fazendas/${FAZENDA_ID()}/animais`, dados),
  atualizar: (id, dados) => api.put(`/animais/${id}`, dados),
  remover: (id) => api.del(`/animais/${id}`),
  enviarFoto: (id, arquivo) => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);
    return api.postForm(`/animais/${id}/foto`, formData);
  },
};
