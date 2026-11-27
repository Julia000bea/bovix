import { api, FAZENDA_ID } from "./api";
import { descrever, provisorio } from "./provisorio";

export const financeiroService = {
  listarPorPeriodo: (inicio, fim, opcoes) => api.get(`/fazendas/${FAZENDA_ID()}/financeiro?inicio=${inicio}&fim=${fim}`, opcoes),
  // sem internet, fica guardado no aparelho e é enviado quando a conexão voltar
  criar: (dados) =>
    api.post(`/fazendas/${FAZENDA_ID()}/financeiro`, dados, {
      colecao: "financeiro",
      descricao: descrever.financeiro(dados),
      provisorio: (localId) => provisorio(localId, { ...dados, valor: Number(dados.valor) || 0 }),
    }),
  remover: (id) => api.del(`/financeiro/${id}`),
};
