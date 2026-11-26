import { inputCls, inputStyle } from "./ui";

// valor usado quando o registro vale para todos os animais ativos (opção "todoRebanho")
export const TODO_REBANHO = "todos";

export default function AnimalPicker({ animais, value, onChange, todoRebanho = false }) {
  const ativos = animais.filter((a) => a.status === "Ativa");
  return (
    <select className={inputCls} style={inputStyle} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Selecione o animal…</option>
      {todoRebanho && ativos.length > 0 && (
        <option value={TODO_REBANHO}>Todo o rebanho ({ativos.length} {ativos.length === 1 ? "animal ativo" : "animais ativos"})</option>
      )}
      {ativos.map((a) => (
        <option key={a.id} value={a.id}>#{a.brinco} — {a.nome || "sem nome"}</option>
      ))}
    </select>
  );
}
