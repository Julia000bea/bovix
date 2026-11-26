import { useCallback, useEffect, useState } from "react";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";
import { Field, EmptyState, ErrorText, Pill, inputCls, inputStyle } from "../components/ui";
import { C, fMono } from "../utils/tokens";
import { addDays, formatDate, todayStr } from "../utils/date";
import { TIPOS_OCORRENCIA, ocorrenciaService } from "../services/ocorrenciaService";

const novaOcorrencia = () => ({ data: todayStr(), tipo: "agua", descricao: "" });
const rotuloTipo = (v) => TIPOS_OCORRENCIA.find((t) => t.v === v)?.l || v;

/** Ocorrências do dia (falta de água, quebra de equipamento...) que ajudam a explicar a produção. */
export default function Ocorrencias() {
  const [dias, setDias] = useState(90);
  const [lista, setLista] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState("");
  const [erro, setErro] = useState("");
  const [form, setForm] = useState(null); // null = fechado; { id? , ... } = criando/editando
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErroCarga("");
    try {
      setLista(await ocorrenciaService.listar(addDays(todayStr(), -dias), todayStr()));
    } catch (e) {
      setErroCarga(e.message || "Não foi possível carregar as ocorrências.");
    } finally {
      setCarregando(false);
    }
  }, [dias]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const salvar = async () => {
    if (!form.descricao.trim()) {
      setErro("Descreva a ocorrência.");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const dados = { data: form.data, tipo: form.tipo, descricao: form.descricao };
      if (form.id) {
        const atualizada = await ocorrenciaService.atualizar(form.id, dados);
        setLista((prev) => prev.map((o) => (o.id === form.id ? atualizada : o)));
      } else {
        const criada = await ocorrenciaService.criar(dados);
        setLista((prev) => [criada, ...prev]);
      }
      setForm(null);
    } catch (e) {
      setErro(e.message || "Não foi possível salvar a ocorrência.");
    } finally {
      setSalvando(false);
    }
  };

  const remover = async (o) => {
    if (!window.confirm("Remover esta ocorrência?")) return;
    setErro("");
    try {
      await ocorrenciaService.remover(o.id);
      setLista((prev) => prev.filter((x) => x.id !== o.id));
    } catch (e) {
      setErro(e.message || "Não foi possível remover a ocorrência.");
    }
  };

  const ordenadas = [...lista].sort((a, b) => (a.data < b.data ? 1 : -1));

  return (
    <div className="flex flex-col gap-4">
      <ErrorText text={erro} />
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm" style={{ color: C.inkSoft }}>
          Registre o que aconteceu de diferente no dia. Isso ajuda a explicar quedas ou picos de produção.
        </p>
        <div className="flex items-center gap-2">
          <select className={inputCls} style={inputStyle} value={dias} onChange={(e) => setDias(Number(e.target.value))}>
            <option value={30}>Últimos 30 dias</option>
            <option value={90}>Últimos 90 dias</option>
            <option value={365}>Último ano</option>
          </select>
          <button onClick={() => setForm(novaOcorrencia())} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.brinco, color: "#fff" }}>
            <Plus size={16} /> Nova ocorrência
          </button>
        </div>
      </div>

      {form && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          <Field label="Data"><input type="date" max={todayStr()} className={inputCls} style={inputStyle} value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></Field>
          <Field label="Tipo">
            <select className={inputCls} style={inputStyle} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {TIPOS_OCORRENCIA.map((t) => <option key={t.v} value={t.v}>{t.l}</option>)}
            </select>
          </Field>
          <div className="sm:col-span-2">
            <Field label="Descrição *">
              <textarea rows={2} maxLength={500} className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} placeholder="ex.: bomba d'água queimou, bebedouros vazios até as 14h" />
            </Field>
          </div>
          <div className="sm:col-span-2 flex justify-end gap-2">
            <button onClick={() => setForm(null)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={salvar} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : form.id ? "Salvar alterações" : "Salvar ocorrência"}
            </button>
          </div>
        </div>
      )}

      {carregando ? (
        <div className="flex items-center gap-2 text-sm" style={{ color: C.inkSoft }}><Loader2 size={16} className="animate-spin" /> Carregando…</div>
      ) : erroCarga ? (
        <div className="rounded-xl border p-4 text-sm" style={{ borderColor: C.cio, color: C.cio }}>
          {erroCarga} <button onClick={carregar} className="underline ml-1">Tentar de novo</button>
        </div>
      ) : ordenadas.length === 0 ? (
        <EmptyState text="Nenhuma ocorrência no período." />
      ) : (
        <div className="flex flex-col gap-2">
          {ordenadas.map((o) => (
            <div key={o.id} className="rounded-xl border bg-white p-3 flex items-start gap-3" style={{ borderColor: C.line }}>
              <span className="text-xs rounded-md px-2 py-1 shrink-0" style={{ ...fMono, backgroundColor: C.pasto50, color: C.pasto900 }}>{formatDate(o.data)}</span>
              <div className="min-w-0 flex-1">
                <Pill color={C.terra}>{rotuloTipo(o.tipo)}</Pill>
                <p className="text-sm mt-1" style={{ color: C.ink }}>{o.descricao}</p>
              </div>
              <button onClick={() => setForm({ id: o.id, data: o.data, tipo: o.tipo, descricao: o.descricao })} className="p-1.5 rounded-md hover:bg-gray-100" title="Editar"><Pencil size={16} color={C.inkSoft} /></button>
              <button onClick={() => remover(o)} className="p-1.5 rounded-md hover:bg-gray-100" title="Remover"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
