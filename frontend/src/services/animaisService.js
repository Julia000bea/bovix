import { api, FAZENDA_ID } from "./api";
import { reduzirImagem } from "../utils/imagem";

export const animaisService = {
  listar: () => api.get(`/fazendas/${FAZENDA_ID()}/animais`),
  buscar: (id) => api.get(`/animais/${id}`),
  criar: (dados) => api.post(`/fazendas/${FAZENDA_ID()}/animais`, dados),
  atualizar: (id, dados) => api.put(`/animais/${id}`, dados),
  remover: (id) => api.del(`/animais/${id}`),
  enviarFoto: async (id, arquivo) => {
    const reduzida = await reduzirImagem(arquivo);
    const formData = new FormData();
    formData.append("arquivo", reduzida);
    return api.postForm(`/animais/${id}/foto`, formData);
  },
};
