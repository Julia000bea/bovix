import { useState } from "react";
import { Baby, HeartPulse, AlertTriangle, Syringe, Milk, Stethoscope, Check, Loader2, X } from "lucide-react";
import { C, fDisplay } from "../utils/tokens";
import { formatDate, todayStr } from "../utils/date";

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

export default function QuadroDoDia({ alerts, carregando = false, erro = null, onTentarDeNovo, onDiagnosticar }) {
  const semDados = erro || (carregando && alerts.length === 0);
  const [aviso, setAviso] = useState("");
  return (
    <div className="rounded-2xl p-5 sm:p-6" style={{ background: `linear-gradient(155deg, ${C.pasto900} 0%, ${C.pasto700} 100%)` }}>
      <div className="flex items-center justify-between mb-4">
        <h2 style={{ ...fDisplay, color: C.pasto50, fontWeight: 700 }} className="text-lg sm:text-xl">Quadro do dia</h2>
        <span style={{ fontFamily: "'IBM Plex Mono', monospace", color: "#C9D3C4" }} className="text-xs">
          {erro ? "indisponível" : semDados ? "carregando…" : alerts.length === 0 ? "tudo em dia" : `${alerts.length} pendência(s)`}
        </span>
      </div>

      {erro ? (
        <div className="rounded-lg px-3 py-3" style={{ backgroundColor: "#5C231D" }}>
          <p style={{ color: "#F6E4DD" }} className="text-sm font-semibold">Não foi possível carregar os alertas de hoje.</p>
          <p style={{ color: "#F6E4DD" }} className="text-xs mt-1">
            Isso não quer dizer que está tudo em dia: confira carências e partos nas abas Saúde e Reprodução. ({erro})
          </p>
          {onTentarDeNovo && (
            <button onClick={onTentarDeNovo} className="mt-2 px-3 py-1.5 rounded-md text-xs font-semibold" style={{ backgroundColor: "#F6E4DD", color: "#5C231D" }}>
              Tentar de novo
            </button>
          )}
        </div>
      ) : semDados ? (
        <p style={{ color: "#DCE5D6" }} className="text-sm">Carregando os alertas de hoje…</p>
      ) : alerts.length === 0 && !aviso ? (
        <p style={{ color: "#DCE5D6" }} className="text-sm">Nenhum alerta de reprodução, carência ou vacina para os próximos dias. 🌱</p>
      ) : (
        <div className="flex flex-col gap-2">
          {aviso && (
            <p className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm" style={{ backgroundColor: "#ffffff1f", color: C.pasto50 }} role="status">
              <Check size={16} /> {aviso}
              <button onClick={() => setAviso("")} className="ml-auto p-0.5" aria-label="Fechar aviso"><X size={14} /></button>
            </p>
          )}
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
                  {al.icone === "diagnostico" && onDiagnosticar && (
                    <AcaoDiagnostico alerta={al} onDiagnosticar={onDiagnosticar} onSucesso={setAviso} />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

/**
 * Registra o diagnóstico de prenhez pendente sem sair do Quadro do dia.
 * Dois passos (escolher o resultado e confirmar) para não registrar com um toque por engano.
 */
function AcaoDiagnostico({ alerta, onDiagnosticar, onSucesso }) {
  const [resultado, setResultado] = useState(null); // null | "prenhe" | "vazia"
  const [data, setData] = useState(todayStr());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  const confirmar = async () => {
    setSalvando(true);
    setErro("");
    try {
      const criado = await onDiagnosticar(alerta.animal.id, resultado, data);
      const vaca = `#${alerta.animal.brinco}${alerta.animal.nome ? " " + alerta.animal.nome : ""}`;
      onSucesso(resultado === "prenhe"
        ? `${vaca}: prenhe${criado?.previsaoParto ? ` — parto previsto para ${formatDate(criado.previsaoParto)}` : ""}.`
        : `${vaca}: vazia. Fique de olho no retorno do cio.`);
    } catch (e) {
      setErro(e.message || "Não foi possível registrar o diagnóstico.");
      setSalvando(false);
    }
  };

  const botao = "px-2.5 py-1 rounded-md text-xs font-semibold";
  if (!resultado) {
    return (
      <div className="flex flex-wrap items-center gap-2 mt-2">
        <span className="text-xs" style={{ color: "#C9D3C4" }}>Resultado do diagnóstico:</span>
        <button onClick={() => setResultado("prenhe")} className={botao} style={{ backgroundColor: C.pasto50, color: C.pasto900 }}>Prenhe</button>
        <button onClick={() => setResultado("vazia")} className={botao} style={{ backgroundColor: "#ffffff1f", color: C.pasto50, border: "1px solid #ffffff40" }}>Vazia</button>
      </div>
    );
  }
  return (
    <div className="flex flex-wrap items-center gap-2 mt-2">
      <span className="text-xs" style={{ color: C.pasto50 }}>
        Registrar como <strong>{resultado === "prenhe" ? "PRENHE" : "VAZIA"}</strong> em
      </span>
      <input type="date" value={data} max={todayStr()} onChange={(e) => e.target.value && setData(e.target.value)}
        className="rounded-md px-2 py-0.5 text-xs" style={{ backgroundColor: C.pasto50, color: C.ink }} aria-label="Data do diagnóstico" />
      <button onClick={confirmar} disabled={salvando} className={`${botao} flex items-center gap-1`} style={{ backgroundColor: C.brinco, color: "#fff" }}>
        {salvando ? <Loader2 size={12} className="animate-spin" /> : <Check size={12} />} Confirmar
      </button>
      <button onClick={() => { setResultado(null); setErro(""); }} disabled={salvando} className={botao} style={{ color: "#C9D3C4" }}>Cancelar</button>
      {erro && <p className="w-full text-xs" style={{ color: "#F6C9C0" }} role="alert">{erro}</p>}
    </div>
  );
}
