import QuadroDoDia from "../components/QuadroDoDia";
import InsightsPanel from "../components/InsightsPanel";
import { C } from "../utils/tokens";
import { todayStr } from "../utils/date";
import { litrosPorDia } from "../utils/producao";

export default function Dashboard({ data, alerts, quadro, insights, setTab }) {
  const { animais, ordenha } = data;
  const ativos = animais.filter((a) => a.status === "Ativa");
  const lactantes = ativos.filter((a) => a.categoria === "Vaca lactante").length;
  const secas = ativos.filter((a) => a.categoria === "Vaca seca").length;
  const hoje = todayStr();
  const litrosHoje = litrosPorDia(ordenha)[hoje] || 0;

  return (
    <div className="flex flex-col gap-5">
      <QuadroDoDia alerts={alerts} carregando={quadro?.carregando} erro={quadro?.erro} onTentarDeNovo={quadro?.recarregar} />
      <InsightsPanel insights={insights} />

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: "Animais ativos", value: ativos.length, color: C.pasto900, onClick: () => setTab("rebanho") },
          { label: "Vacas lactantes", value: lactantes, color: C.leite, onClick: () => setTab("rebanho") },
          { label: "Vacas secas", value: secas, color: C.terra, onClick: () => setTab("rebanho") },
          { label: "Litros hoje", value: litrosHoje.toLocaleString("pt-BR"), color: C.brincoDark, onClick: () => setTab("ordenha") },
        ].map((s) => (
          <button key={s.label} onClick={s.onClick} className="text-left rounded-xl p-4 bg-white border hover:shadow-sm transition-shadow" style={{ borderColor: C.line }}>
            <p style={{ fontFamily: "'IBM Plex Mono', monospace", color: s.color }} className="text-2xl font-semibold">{s.value}</p>
            <p style={{ color: C.inkSoft }} className="text-xs mt-1">{s.label}</p>
          </button>
        ))}
      </div>
    </div>
  );
}
