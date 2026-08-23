import { LayoutGrid, Tag, HeartPulse, Syringe, Milk, Wallet } from "lucide-react";
import { C } from "../utils/tokens";

export const TABS = [
  { id: "dashboard", label: "Painel do dia", icon: LayoutGrid },
  { id: "rebanho", label: "Rebanho", icon: Tag },
  { id: "reproducao", label: "Reprodução", icon: HeartPulse },
  { id: "saude", label: "Saúde", icon: Syringe },
  { id: "ordenha", label: "Ordenha", icon: Milk },
  { id: "financeiro", label: "Financeiro", icon: Wallet },
];

export default function TabNav({ tab, setTab, alertCount }) {
  return (
    <nav className="sticky top-0 z-10 px-4 sm:px-6 border-b" style={{ backgroundColor: C.pasto50, borderColor: C.line }}>
      <div className="max-w-5xl mx-auto flex gap-1 overflow-x-auto py-2">
        {TABS.map((t) => {
          const Icon = t.icon;
          const active = tab === t.id;
          return (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm whitespace-nowrap transition-colors"
              style={{ backgroundColor: active ? C.pasto900 : "transparent", color: active ? C.pasto50 : C.inkSoft, fontWeight: 600 }}
            >
              <Icon size={16} />
              {t.label}
              {t.id === "dashboard" && alertCount > 0 && (
                <span className="ml-1 text-[10px] rounded-full px-1.5 py-0.5" style={{ backgroundColor: active ? C.brinco : C.cio, color: "#fff" }}>
                  {alertCount}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
}
