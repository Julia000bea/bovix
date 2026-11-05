import { useState } from "react";
import { Plus, Syringe, Trash2 } from "lucide-react";
import { Field, EmptyState, ErrorText, inputCls, inputStyle } from "../components/ui";
import AnimalPicker from "../components/AnimalPicker";
import { C, TIPOS_SAUDE, fMono } from "../utils/tokens";
import { formatDate } from "../utils/date";
import { novoEventoSaude } from "../models/EventoSaude";
import { rotuloAnimal } from "../models/Animal";
import { saudeService } from "../services/saudeService";

export default function Saude({ data }) {
  const { animais, saude, setSaude } = data;
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(novoEventoSaude());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  const add = async () => {
    if (!form.animalId || !form.produto) return;
    setSalvando(true);
    setErro("");
    try {
      const criado = await saudeService.criar(form.animalId, form);
      setSaude((prev) => [...prev, criado]);
      setForm(novoEventoSaude());
      setOpen(false);
    } catch (e) {
      setErro(e.message || "Não foi possível salvar o registro.");
    } finally {
      setSalvando(false);
    }
  };

  const remove = async (id) => {
    if (!window.confirm("Remover este registro de saúde?")) return;
    setErro("");
    try {
      await saudeService.remover(id);
      setSaude((prev) => prev.filter((e) => e.id !== id));
    } catch (e) {
      setErro(e.message || "Não foi possível remover o registro.");
    }
  };

  const eventos = [...saude].sort((a, b) => (a.data < b.data ? 1 : -1));

  return (
    <div className="flex flex-col gap-4">
      <ErrorText text={erro} />
      <div className="flex justify-end">
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.leite, color: "#fff" }}>
          <Plus size={16} /> Novo registro
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          <Field label="Animal *"><AnimalPicker animais={animais} value={form.animalId} onChange={(v) => setForm({ ...form, animalId: v })} /></Field>
          <Field label="Tipo">
            <select className={inputCls} style={inputStyle} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {TIPOS_SAUDE.map((t) => <option key={t.v} value={t.v}>{t.l}</option>)}
            </select>
          </Field>
          <Field label="Produto / medicamento *"><input className={inputCls} style={inputStyle} value={form.produto} onChange={(e) => setForm({ ...form, produto: e.target.value })} /></Field>
          <Field label="Data da aplicação"><input type="date" className={inputCls} style={inputStyle} value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></Field>
          <Field label="Carência do leite (dias)"><input type="number" min="0" className={inputCls} style={inputStyle} value={form.carenciaLeiteDias} onChange={(e) => setForm({ ...form, carenciaLeiteDias: e.target.value })} placeholder="ex: 4" /></Field>
          <Field label="Repetir aplicação em (dias)"><input type="number" min="1" className={inputCls} style={inputStyle} value={form.proximaAplicacaoDias} onChange={(e) => setForm({ ...form, proximaAplicacaoDias: e.target.value })} placeholder="ex: 365 (anual)" /></Field>
          <div className="sm:col-span-2">
            <Field label="Observações"><input className={inputCls} style={{ ...inputStyle, width: "100%" }} value={form.observacoes} onChange={(e) => setForm({ ...form, observacoes: e.target.value })} /></Field>
          </div>
          <div className="sm:col-span-2 flex justify-end gap-2">
            <button onClick={() => setOpen(false)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={add} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : "Salvar registro"}
            </button>
          </div>
        </div>
      )}

      {eventos.length === 0 ? (
        <EmptyState text="Nenhuma vacina, vermífugo ou medicamento registrado ainda." />
      ) : (
        <div className="flex flex-col gap-2">
          {eventos.map((e) => (
            <div key={e.id} className="rounded-xl border bg-white p-3 flex items-center gap-3" style={{ borderColor: C.line }}>
              <span className="w-9 h-9 rounded-full flex items-center justify-center shrink-0" style={{ backgroundColor: C.leite + "22" }}>
                <Syringe size={16} color={C.leite} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium" style={{ color: C.ink }}>{e.produto} — {rotuloAnimal(animais.find((a) => a.id === e.animalId))}</p>
                <p className="text-xs" style={{ color: C.inkSoft }}>
                  {formatDate(e.data)} · {TIPOS_SAUDE.find((t) => t.v === e.tipo)?.l}
                  {e.carenciaLeiteDias && ` · carência ${e.carenciaLeiteDias}d`}
                  {e.observacoes && ` · ${e.observacoes}`}
                </p>
              </div>
              <button onClick={() => remove(e.id)} className="p-1.5 rounded-md hover:bg-gray-100"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
