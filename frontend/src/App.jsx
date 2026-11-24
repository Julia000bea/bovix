import { useState } from "react";
import { Loader2, LogOut } from "lucide-react";
import Header from "./components/Header";
import TabNav from "./components/TabNav";
import SettingsPanel from "./components/SettingsPanel";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Rebanho from "./pages/Rebanho";
import Reproducao from "./pages/Reproducao";
import Saude from "./pages/Saude";
import Ordenha from "./pages/Ordenha";
import Financeiro from "./pages/Financeiro";
import { useAuth } from "./hooks/useAuth";
import { useFarmData } from "./hooks/useFarmData";
import { useAlerts } from "./hooks/useAlerts";
import { useInsights } from "./hooks/useInsights";
import { C, fBody } from "./utils/tokens";

export default function App() {
  const auth = useAuth();

  if (auth.carregando) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: C.pasto50, ...fBody }}>
        <Loader2 className="animate-spin" size={22} color={C.pasto900} />
      </div>
    );
  }

  if (!auth.autenticado) {
    return <Login auth={auth} />;
  }

  return <AppLogado auth={auth} />;
}

function AppLogado({ auth }) {
  const farm = useFarmData();
  const [tab, setTab] = useState("dashboard");
  const [settingsOpen, setSettingsOpen] = useState(false);

  const quadro = useAlerts(farm);
  const alerts = quadro.alertas;
  const insights = useInsights(farm, alerts);

  if (farm.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: C.pasto50, ...fBody }}>
        <div className="flex items-center gap-2" style={{ color: C.pasto900 }}>
          <Loader2 className="animate-spin" size={20} />
          Carregando dados da fazenda…
        </div>
      </div>
    );
  }

  if (farm.erro) {
    return (
      <div className="min-h-screen flex items-center justify-center px-6 text-center" style={{ backgroundColor: C.pasto50, ...fBody }}>
        <div>
          <p style={{ color: C.cio }} className="font-semibold mb-2">Não foi possível falar com a API do Bovix.</p>
          <p style={{ color: C.inkSoft }} className="text-sm mb-4">{farm.erro}</p>
          <button onClick={farm.recarregar} className="px-4 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>Tentar novamente</button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: C.pasto50, ...fBody }}>
      <Header onToggleSettings={() => setSettingsOpen((o) => !o)} usuario={auth.usuario} onLogout={auth.logout} />
      {settingsOpen && (
        <div className="px-4 sm:px-6" style={{ backgroundColor: C.pasto900 }}>
          <div className="max-w-5xl mx-auto pb-4">
            <SettingsPanel config={farm.config} setConfig={farm.setConfig} onClose={() => setSettingsOpen(false)} />
          </div>
        </div>
      )}
      <TabNav tab={tab} setTab={setTab} alertCount={alerts.length} />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 py-6">
        {tab === "dashboard" && <Dashboard data={farm} alerts={alerts} quadro={quadro} insights={insights} setTab={setTab} />}
        {tab === "rebanho" && <Rebanho data={farm} />}
        {tab === "reproducao" && <Reproducao data={farm} />}
        {tab === "saude" && <Saude data={farm} />}
        {tab === "ordenha" && <Ordenha data={farm} />}
        {tab === "financeiro" && <Financeiro data={farm} />}
      </main>
    </div>
  );
}
