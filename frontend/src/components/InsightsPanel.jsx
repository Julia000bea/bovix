import { Lightbulb, Baby, HeartPulse, TrendingDown, TrendingUp, Search } from "lucide-react";
import { C, fDisplay } from "../utils/tokens";

const insightCfg = {
  positivo: { bg: "#3F6B4E", fg: "#E7F1E9" },
  atencao: { bg: C.brinco, fg: "#FCEFDD" },
  alerta: { bg: C.cio, fg: "#FBEAE6" },
};
const iconFor = { cio: HeartPulse, parto: Baby, queda: TrendingDown, alta: TrendingUp, dados: Search };

/** "O que os números dizem": insights de apoio à decisão, derivados dos dados já registrados. */
export default function InsightsPanel({ insights }) {
  if (insights.length === 0) return null;
  return (
    <div className="rounded-2xl border bg-white p-5 sm:p-6" style={{ borderColor: C.line }}>
      <div className="flex items-center gap-2 mb-3">
        <Lightbulb size={18} color={C.brincoDark} />
        <h2 style={{ ...fDisplay, color: C.ink, fontWeight: 700 }} className="text-lg">O que os números dizem</h2>
      </div>
      <div className="flex flex-col gap-2">
        {insights.map((ins) => {
          const cfg = insightCfg[ins.tipo];
          const Icon = iconFor[ins.icone] || Lightbulb;
          return (
            <div key={ins.id} className="flex items-start gap-3 rounded-lg px-3 py-2.5" style={{ backgroundColor: C.pasto50 }}>
              <span className="flex items-center justify-center rounded-full shrink-0 mt-0.5" style={{ backgroundColor: cfg.bg, width: 28, height: 28 }}>
                <Icon size={14} color={cfg.fg} />
              </span>
              <p className="text-sm leading-snug" style={{ color: C.ink }}>{ins.texto}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
