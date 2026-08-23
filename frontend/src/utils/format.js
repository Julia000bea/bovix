export const brl = (v) =>
  Number(v || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const litros = (v) => `${Number(v || 0).toLocaleString("pt-BR")} L`;
