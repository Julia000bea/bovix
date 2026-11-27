import { api, FAZENDA_ID } from "./api";
import { descrever, numeroOuNull, provisorio } from "./provisorio";

export const saudeService = {
  listarPorFazenda: () => api.get(`/fazendas/${FAZENDA_ID()}/saude`),
  // sem internet, ficam guardados no aparelho e são enviados quando a conexão voltar
  criar: (animalId, dados) =>
    api.post(`/animais/${animalId}/saude`, dados, {
      colecao: "saude",
      descricao: descrever.saude(dados),
      provisorio: (localId) => provisorio(localId, dados, { animalId: numeroOuNull(animalId) }),
    }),
  // para o rebanho todo, a lista de animais é decidida pela API no envio: até lá a tela não
  // mostra um registro por animal, só o aviso de "aguardando envio"
  criarParaRebanho: (dados) =>
    api.post(`/fazendas/${FAZENDA_ID()}/saude/rebanho`, dados, {
      colecao: "saude",
      descricao: descrever.saude(dados, true),
      provisorio: () => [],
    }),
  remover: (id) => api.del(`/saude/${id}`),
};
