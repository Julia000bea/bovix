import { api, FAZENDA_ID } from "./api";

export const painelService = {
  alertas: () => api.get(`/fazendas/${FAZENDA_ID()}/alertas`),
};
