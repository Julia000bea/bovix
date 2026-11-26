import { Settings, LogOut } from "lucide-react";
import { C, fMono } from "../utils/tokens";
import { formatDate, todayStr } from "../utils/date";
import BrandLogo from "./BrandLogo";

export default function Header({ onToggleSettings, usuario, onLogout }) {
  return (
    <header className="px-4 sm:px-6 pt-6 pb-4" style={{ backgroundColor: C.pasto900 }}>
      <div className="max-w-5xl mx-auto flex items-center justify-between">
        <BrandLogo light compact />
        <div className="flex items-center gap-2">
          <span className="hidden sm:inline text-xs px-2 py-1 rounded" style={{ ...fMono, color: C.pasto50, backgroundColor: "#ffffff18" }}>
            {formatDate(todayStr())}
          </span>
          {usuario && (
            <span className="hidden sm:inline text-xs px-2 py-1 rounded" style={{ color: "#DCE5D6", backgroundColor: "#ffffff18" }}>
              {usuario.nome}
            </span>
          )}
          <button onClick={onToggleSettings} className="p-2 rounded-lg" style={{ backgroundColor: "#ffffff18" }} title="Configurações">
            <Settings size={16} color={C.pasto50} />
          </button>
          {onLogout && (
            <button onClick={onLogout} className="p-2 rounded-lg" style={{ backgroundColor: "#ffffff18" }} title="Sair">
              <LogOut size={16} color={C.pasto50} />
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
