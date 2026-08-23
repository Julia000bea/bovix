import { api, FAZENDA_ID } from "./api";

export const configService = {
  buscar: () => api.get(`/fazendas/${FAZENDA_ID()}/configuracoes`),
  atualizar: (dados) => api.put(`/fazendas/${FAZENDA_ID()}/configuracoes`, dados),
  testarNotificacao: () => api.post(`/fazendas/${FAZENDA_ID()}/configuracoes/notificacao/testar`, {}),
};
