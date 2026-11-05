// Funções utilitárias de data usadas em toda a aplicação.
// Sempre trabalhamos com strings 'YYYY-MM-DD' no fuso LOCAL do navegador — toISOString() é UTC
// e, no Brasil, viraria o dia seguinte depois das 21h.

const pad = (n) => String(n).padStart(2, "0");
const formatLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const partes = (dateStr) => dateStr.split("-").map(Number);

export const todayStr = () => formatLocal(new Date());

export const addDays = (dateStr, n) => {
  const [y, m, d] = partes(dateStr);
  return formatLocal(new Date(y, m - 1, d + n));
};

export const daysBetween = (fromStr, toStr) => {
  const [y1, m1, d1] = partes(fromStr);
  const [y2, m2, d2] = partes(toStr);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
};

export const formatDate = (dateStr) => {
  if (!dateStr) return "—";
  const [y, m, d] = dateStr.split("-");
  return `${d}/${m}/${y}`;
};
