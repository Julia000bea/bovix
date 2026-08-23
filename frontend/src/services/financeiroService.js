import { api, FAZENDA_ID } from "./api";

export const financeiroService = {
  listarPorPeriodo: (inicio, fim) => api.get(`/fazendas/${FAZENDA_ID()}/financeiro?inicio=${inicio}&fim=${fim}`),
  criar: (dados) => api.post(`/fazendas/${FAZENDA_ID()}/financeiro`, dados),
  remover: (id) => api.del(`/financeiro/${id}`),
};
