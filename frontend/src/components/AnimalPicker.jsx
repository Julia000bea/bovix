import { inputCls, inputStyle } from "./ui";

export default function AnimalPicker({ animais, value, onChange }) {
  const ativos = animais.filter((a) => a.status === "Ativa");
  return (
    <select className={inputCls} style={inputStyle} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Selecione o animal…</option>
      {ativos.map((a) => (
        <option key={a.id} value={a.id}>#{a.brinco} — {a.nome || "sem nome"}</option>
      ))}
    </select>
  );
}
