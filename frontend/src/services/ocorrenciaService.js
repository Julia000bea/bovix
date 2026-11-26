import { api, FAZENDA_ID } from "./api";

export const TIPOS_OCORRENCIA = [
  { v: "alimentacao", l: "Alimentação" },
  { v: "agua", l: "Água" },
  { v: "equipamento", l: "Equipamento" },
  { v: "clima", l: "Clima" },
  { v: "sanidade", l: "Sanidade" },
  { v: "manejo", l: "Manejo" },
  { v: "outro", l: "Outro" },
];

export const ocorrenciaService = {
  listar: (inicio, fim) => api.get(`/fazendas/${FAZENDA_ID()}/ocorrencias?inicio=${inicio}&fim=${fim}`),
  criar: (dados) => api.post(`/fazendas/${FAZENDA_ID()}/ocorrencias`, dados),
  atualizar: (id, dados) => api.put(`/ocorrencias/${id}`, dados),
  remover: (id) => api.del(`/ocorrencias/${id}`),
};
