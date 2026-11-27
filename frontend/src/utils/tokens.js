// Paleta e tipografia do Bovix — mantidas centralizadas para reuso em todas as páginas.
export const C = {
  pasto900: "#1F3A2E",
  pasto700: "#2C4E3C",
  pasto50: "#F5F7F0",
  card: "#FFFFFF",
  brinco: "#DB8A2C",
  brincoDark: "#B76F1F",
  cobre: "#A95B1B",   // botão principal da tela de login
  creme: "#F5F0E6",   // fundo do círculo do logo
  marca: "#3A1414",   // cor da vaca do logo
  cio: "#B0473C",
  leite: "#4C7A93",
  terra: "#6B4F3C",
  ink: "#22291D",
  inkSoft: "#5B6355",
  line: "#E1E3D8",
};

// Cores das séries dos gráficos (ordem fixa; validadas para daltonismo e contraste sobre fundo branco).
// As cores da marca (leite, brinco) ficam acinzentadas ou com pouco contraste como séries.
export const SERIE = {
  s1: "#2a78d6", // azul
  s2: "#eb6834", // laranja
  referencia: "#8a8f86",
  grade: "#E1E3D8",
};

export const fDisplay ={ fontFamily: "'Zilla Slab', serif" };
export const fBody = { fontFamily: "'Inter', sans-serif" };
export const fMono = { fontFamily: "'IBM Plex Mono', monospace" };

export const CATEGORIAS_FEMEA = ["Bezerra", "Novilha", "Vaca lactante", "Vaca seca"];
export const CATEGORIAS_MACHO = ["Bezerro", "Touro"];
export const CATEGORIAS = [...CATEGORIAS_FEMEA, ...CATEGORIAS_MACHO];
/** Bezerro ou touro: fica fora de reprodução e ordenha. */
export const ehMacho = (animal) => CATEGORIAS_MACHO.includes(animal?.categoria);
export const RACAS = ["Holandês", "Jersey", "Red Angus", "Angus Preto"];
export const STATUS_ANIMAL = ["Ativa", "Vendida", "Morta"];
export const CATEGORIAS_GASTO = ["Ração", "Insumos", "Medicamentos", "Mão de obra", "Manutenção", "Outros"];
export const CATEGORIAS_RECEITA = ["Venda de leite", "Outros"];
/** Lançamentos antigos (sem tipo) são despesas. */
export const ehDespesa = (lancamento) => (lancamento.tipo || "despesa") === "despesa";
export const TIPOS_REPRO = [
  { v: "cio_detectado", l: "Cio detectado" },
  { v: "cio_confirmado", l: "Cio confirmado" },
  { v: "reproducao", l: "Reprodução (Touro)" },
  { v: "inseminacao", l: "Inseminação" },
  { v: "diagnostico_prenhez", l: "Diagnóstico de prenhez" },
  { v: "prenhez_confirmada", l: "Prenhez confirmada" },
  { v: "secagem", l: "Secagem" },
  { v: "pre_parto", l: "Pré-parto" },
  { v: "parto", l: "Parto" },
  { v: "perda_gestacional", l: "Perda gestacional (aborto)" },
];

export const STATUS_REPRODUCAO = [
  "No cio",
  "Cio confirmado",
  "Reproduzida",
  "Prenhe",
  "Seca",
  "Vaca lactante",
];
export const TIPOS_SAUDE = [
  { v: "vacina", l: "Vacina" },
  { v: "medicamento", l: "Medicamento" },
  { v: "vermifugo", l: "Vermífugo" },
  { v: "outro", l: "Outro tratamento" },
];
