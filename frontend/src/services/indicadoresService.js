import { api, authStorage, FAZENDA_ID } from "./api";

const base = () => `/fazendas/${FAZENDA_ID()}`;

export const indicadoresService = {
  serie: (inicio, fim) => api.get(`${base()}/indicadores/serie?inicio=${inicio}&fim=${fim}`),
  reproducao: () => api.get(`${base()}/indicadores/reproducao`),
  projecao: () => api.get(`${base()}/indicadores/projecao`),
  exportar: (inicio, fim) =>
    api.baixar(`${base()}/indicadores/exportar?inicio=${inicio}&fim=${fim}`, `bovix_dados_${inicio}_${fim}.csv`),
  clima: (inicio, fim) => api.get(`${base()}/clima?inicio=${inicio}&fim=${fim}`),
  importarClima: (arquivo) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    return api.postForm(`${base()}/clima/importar`, fd);
  },
};

export const fazendaService = {
  /** A fazenda atual (com latitude, longitude e estação do INMET). */
  atual: async () => {
    const fazendas = await api.get("/fazendas/minhas");
    return fazendas.find((f) => f.id === authStorage.getFazendaId()) || fazendas[0] || null;
  },
  atualizarLocalizacao: (dados) => api.put(`${base()}/localizacao`, dados),
};
