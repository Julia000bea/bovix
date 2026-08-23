// Funções utilitárias de data usadas em toda a aplicação.
// Sempre trabalhamos com strings 'YYYY-MM-DD' para evitar problemas de fuso horário.

export const todayStr = () => new Date().toISOString().slice(0, 10);

export const addDays = (dateStr, n) => {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
};

export const daysBetween = (fromStr, toStr) =>
  Math.round((new Date(toStr + "T00:00:00") - new Date(fromStr + "T00:00:00")) / 86400000);

export const formatDate = (dateStr) => {
  if (!dateStr) return "—";
  const [y, m, d] = dateStr.split("-");
  return `${d}/${m}/${y}`;
};
