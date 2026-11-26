import { useEffect, useState } from "react";
import { Mail, Loader2, CheckCircle2, MapPin } from "lucide-react";
import { Field, inputCls, inputStyle } from "./ui";
import { C } from "../utils/tokens";
import { configService } from "../services/configService";
import { fazendaService } from "../services/indicadoresService";

export default function SettingsPanel({ config, setConfig, onClose }) {
  const [gestacao, setGestacao] = useState(config.gestacaoDias);
  const [cio, setCio] = useState(config.cioDias);
  const [notificacaoAtiva, setNotificacaoAtiva] = useState(!!config.notificacaoAtiva);
  const [notificacaoEmail, setNotificacaoEmail] = useState(config.notificacaoEmail || "");
  const [notificacaoHorario, setNotificacaoHorario] = useState((config.notificacaoHorario || "07:00:00").slice(0, 5));
  const [periodoSeco, setPeriodoSeco] = useState(config.periodoSecoDias ?? 60);
  const [diasVazia, setDiasVazia] = useState(config.diasVaziaAlerta ?? 90);
  const [local, setLocal] = useState({ latitude: "", longitude: "", estacaoInmet: "" });
  const [localOriginal, setLocalOriginal] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [testando, setTestando] = useState(false);
  const [testeOk, setTesteOk] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    fazendaService.atual().then((f) => {
      if (!f) return;
      const l = { latitude: f.latitude ?? "", longitude: f.longitude ?? "", estacaoInmet: f.estacaoInmet ?? "" };
      setLocal(l);
      setLocalOriginal(l);
    }).catch(() => setErro("Não foi possível carregar a localização da fazenda."));
  }, []);

  const salvar = async () => {
    setSalvando(true);
    setErro("");
    try {
      const atualizado = await configService.atualizar({
        gestacaoDias: Number(gestacao) || 283,
        cioDias: Number(cio) || 21,
        notificacaoAtiva,
        notificacaoEmail: notificacaoEmail.trim(), // "" remove o e-mail cadastrado
        notificacaoHorario: notificacaoHorario ? `${notificacaoHorario}:00` : null,
        periodoSecoDias: Number(periodoSeco) || 60,
        diasVaziaAlerta: Number(diasVazia) || 90,
      });
      if (localOriginal && JSON.stringify(local) !== JSON.stringify(localOriginal)) {
        await fazendaService.atualizarLocalizacao({
          latitude: local.latitude === "" ? null : Number(String(local.latitude).replace(",", ".")),
          longitude: local.longitude === "" ? null : Number(String(local.longitude).replace(",", ".")),
          estacaoInmet: local.estacaoInmet.trim(),
        });
      }
      setConfig(atualizado);
      onClose();
    } catch (e) {
      setErro(e.message || "Não foi possível salvar.");
    } finally {
      setSalvando(false);
    }
  };

  const testarAgora = async () => {
    setTestando(true);
    setErro("");
    setTesteOk(false);
    try {
      await configService.testarNotificacao();
      setTesteOk(true);
    } catch (e) {
      setErro(e.message || "Não foi possível enviar o e-mail de teste. Salve as configurações primeiro.");
    } finally {
      setTestando(false);
    }
  };

  return (
    <div className="rounded-xl p-4 flex flex-col gap-4" style={{ backgroundColor: "#ffffff12" }}>
      <div className="grid sm:grid-cols-3 gap-3 items-end">
        <Field label={<span style={{ color: "#DCE5D6" }}>Duração da gestação (dias)</span>}>
          <input type="number" min="1" className={inputCls} style={inputStyle} value={gestacao} onChange={(e) => setGestacao(e.target.value)} />
        </Field>
        <Field label={<span style={{ color: "#DCE5D6" }}>Ciclo de cio (dias)</span>}>
          <input type="number" min="1" className={inputCls} style={inputStyle} value={cio} onChange={(e) => setCio(e.target.value)} />
        </Field>
      </div>
      <p className="text-xs -mt-2" style={{ color: "#B7C2B0" }}>
        Usados para prever partos e janelas de cio. Padrão: 283 dias de gestação e ciclo de 21 dias — ajuste se sua raça tiver uma média diferente.
      </p>

      <div className="grid sm:grid-cols-3 gap-3 items-end">
        <Field label={<span style={{ color: "#DCE5D6" }}>Período seco (dias antes do parto)</span>}>
          <input type="number" min="30" max="120" className={inputCls} style={inputStyle} value={periodoSeco} onChange={(e) => setPeriodoSeco(e.target.value)} />
        </Field>
        <Field label={<span style={{ color: "#DCE5D6" }}>Alerta de vaca vazia após o parto (dias)</span>}>
          <input type="number" min="30" max="400" className={inputCls} style={inputStyle} value={diasVazia} onChange={(e) => setDiasVazia(e.target.value)} />
        </Field>
      </div>
      <p className="text-xs -mt-2" style={{ color: "#B7C2B0" }}>
        Usados nos indicadores: data prevista da secagem (padrão 60 dias antes do parto) e vacas vazias há mais de X dias (padrão 90).
      </p>

      <div className="h-px" style={{ backgroundColor: "#ffffff20" }} />

      <div className="flex items-center gap-2">
        <MapPin size={16} color="#DCE5D6" />
        <span className="text-sm font-semibold" style={{ color: "#DCE5D6" }}>Localização e clima</span>
      </div>
      <div className="grid sm:grid-cols-3 gap-3">
        <Field label={<span style={{ color: "#DCE5D6" }}>Latitude</span>}>
          <input inputMode="decimal" placeholder="-15.789444" className={inputCls} style={inputStyle} value={local.latitude} onChange={(e) => setLocal({ ...local, latitude: e.target.value })} />
        </Field>
        <Field label={<span style={{ color: "#DCE5D6" }}>Longitude</span>}>
          <input inputMode="decimal" placeholder="-47.925833" className={inputCls} style={inputStyle} value={local.longitude} onChange={(e) => setLocal({ ...local, longitude: e.target.value })} />
        </Field>
        <Field label={<span style={{ color: "#DCE5D6" }}>Estação do INMET</span>}>
          <input maxLength={4} placeholder="ex.: A001" className={inputCls} style={inputStyle} value={local.estacaoInmet} onChange={(e) => setLocal({ ...local, estacaoInmet: e.target.value.toUpperCase() })} />
        </Field>
      </div>
      <p className="text-xs -mt-2" style={{ color: "#B7C2B0" }}>
        Use a estação automática mais próxima da fazenda (código com uma letra e três números, no mapa de estações do portal do INMET). O clima e o ITU vêm dela.
      </p>

      <div className="h-px" style={{ backgroundColor: "#ffffff20" }} />

      <div className="flex items-center gap-2">
        <Mail size={16} color="#DCE5D6" />
        <span className="text-sm font-semibold" style={{ color: "#DCE5D6" }}>Relatório diário por e-mail</span>
      </div>

      <label className="flex items-center gap-2 text-sm" style={{ color: "#DCE5D6" }}>
        <input type="checkbox" checked={notificacaoAtiva} onChange={(e) => setNotificacaoAtiva(e.target.checked)} />
        Enviar o quadro do dia automaticamente todo dia
      </label>

      <div className="grid sm:grid-cols-2 gap-3">
        <Field label={<span style={{ color: "#DCE5D6" }}>E-mail de destino</span>}>
          <input type="email" placeholder="voce@fazenda.com" className={inputCls} style={{ ...inputStyle, width: "100%" }} value={notificacaoEmail} onChange={(e) => setNotificacaoEmail(e.target.value)} />
        </Field>
        <Field label={<span style={{ color: "#DCE5D6" }}>Horário do envio</span>}>
          <input type="time" className={inputCls} style={inputStyle} value={notificacaoHorario} onChange={(e) => setNotificacaoHorario(e.target.value)} />
        </Field>
      </div>

      {erro && <p className="text-xs" style={{ color: "#F6C9C0" }}>{erro}</p>}
      {testeOk && (
        <p className="text-xs flex items-center gap-1" style={{ color: "#B7D9BE" }}>
          <CheckCircle2 size={14} /> E-mail de teste enviado — confira sua caixa de entrada.
        </p>
      )}

      <div className="flex flex-wrap justify-between items-center gap-2">
        <button
          onClick={testarAgora}
          // o teste usa o e-mail salvo no servidor: com alteração pendente, salve antes
          disabled={testando || !notificacaoEmail || notificacaoEmail.trim() !== (config.notificacaoEmail || "")}
          title={notificacaoEmail.trim() !== (config.notificacaoEmail || "") ? "Salve o e-mail antes de testar" : undefined}
          className="flex items-center gap-1.5 px-3 py-2 text-sm rounded-lg font-semibold disabled:opacity-40"
          style={{ backgroundColor: "#ffffff18", color: "#DCE5D6" }}
        >
          {testando && <Loader2 size={14} className="animate-spin" />}
          Enviar teste agora
        </button>

        <div className="flex gap-2">
          <button onClick={onClose} className="px-3 py-2 text-sm rounded-lg" style={{ color: "#DCE5D6" }}>Cancelar</button>
          <button onClick={salvar} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.brinco, color: "#fff" }}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </div>
      </div>
    </div>
  );
}
