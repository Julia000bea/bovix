import { Baby, HeartPulse, AlertTriangle, Syringe, Milk, Stethoscope } from "lucide-react";
import { C, fDisplay } from "../utils/tokens";

const urgCfg = {
  atrasado: { bg: "#5C231D", fg: "#F6E4DD", label: "ATRASADO" },
  urgente: { bg: C.cio, fg: "#FBEAE6", label: "URGENTE" },
  ativo: { bg: C.leite, fg: "#EAF3F6", label: "EM ANDAMENTO" },
  proximo: { bg: C.brinco, fg: "#FCEFDD", label: "PRÓXIMO" },
};
const iconFor = {
  parto: Baby, preparto: Baby, secagem: Milk, diagnostico: Stethoscope,
  cio: HeartPulse, carencia: AlertTriangle, saude: Syringe,
};

export default function QuadroDoDia({ alerts }) {
  return (
    <div className="rounded-2xl p-5 sm:p-6" style={{ background: `linear-gradient(155deg, ${C.pasto900} 0%, ${C.pasto700} 100%)` }}>
      <div className="flex items-center justify-between mb-4">
        <h2 style={{ ...fDisplay, color: C.pasto50, fontWeight: 700 }} className="text-lg sm:text-xl">Quadro do dia</h2>
        <span style={{ fontFamily: "'IBM Plex Mono', monospace", color: "#C9D3C4" }} className="text-xs">
          {alerts.length === 0 ? "tudo em dia" : `${alerts.length} pendência(s)`}
        </span>
      </div>

      {alerts.length === 0 ? (
        <p style={{ color: "#DCE5D6" }} className="text-sm">Nenhum alerta de reprodução, carência ou vacina para os próximos dias. 🌱</p>
      ) : (
        <div className="flex flex-col gap-2">
          {alerts.map((al) => {
            const cfg = urgCfg[al.urgencia] || urgCfg.proximo;
            const Icon = iconFor[al.icone] || AlertTriangle;
            return (
              <div key={al.id} className="flex items-center gap-3 rounded-lg px-3 py-2.5" style={{ backgroundColor: "#ffffff12" }}>
                <span className="flex items-center justify-center rounded-full shrink-0" style={{ backgroundColor: cfg.bg, width: 32, height: 32 }}>
                  <Icon size={16} color={cfg.fg} />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span style={{ fontFamily: "'IBM Plex Mono', monospace", color: C.pasto50 }} className="text-xs font-semibold">#{al.animal.brinco}</span>
                    <span style={{ color: "#EDF1E8" }} className="text-sm font-medium truncate">{al.animal.nome || "sem nome"}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded font-bold tracking-wide" style={{ backgroundColor: cfg.bg, color: cfg.fg }}>{cfg.label}</span>
                  </div>
                  <p style={{ color: "#C9D3C4" }} className="text-xs mt-0.5">{al.texto}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
