import { api, FAZENDA_ID } from "./api";
import { descrever, numeroOuNull, provisorio } from "./provisorio";

export const reproducaoService = {
  listarPorFazenda: () => api.get(`/fazendas/${FAZENDA_ID()}/reproducao`),
  // sem internet, fica guardado no aparelho e é enviado quando a conexão voltar
  criar: (animalId, dados) =>
    api.post(`/animais/${animalId}/reproducao`, dados, {
      colecao: "reproducao",
      descricao: descrever.reproducao(dados),
      provisorio: (localId) => provisorio(localId, dados, { animalId: numeroOuNull(animalId) }),
    }),
  confirmarCio: (eventoId) => api.put(`/reproducao/${eventoId}/confirmar-cio`, {}),
  remover: (id) => api.del(`/reproducao/${id}`),
};
