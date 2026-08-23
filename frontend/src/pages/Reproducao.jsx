import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { Field, EmptyState, inputCls, inputStyle } from "../components/ui";
import AnimalPicker from "../components/AnimalPicker";
import { C, TIPOS_REPRO, fMono } from "../utils/tokens";
import { formatDate } from "../utils/date";
import { novoEventoReproducao } from "../models/EventoReproducao";
import { rotuloAnimal } from "../models/Animal";
import { reproducaoService } from "../services/reproducaoService";
import { animaisService } from "../services/animaisService";

export default function Reproducao({ data }) {
  const { animais, setAnimais, reproducao, setReproducao } = data;
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(novoEventoReproducao());
  const [salvando, setSalvando] = useState(false);

  const add = async () => {
    if (!form.animalId) return;
    setSalvando(true);
    try {
      const criado = await reproducaoService.criar(form.animalId, form);
      setReproducao((prev) => [...prev, criado]);

      if (form.tipo === "parto") {
        setAnimais((prev) => prev.map((a) => (a.id === Number(form.animalId) ? { ...a, categoria: "Vaca lactante" } : a)));
      } else if (form.tipo === "secagem") {
        setAnimais((prev) => prev.map((a) => (a.id === Number(form.animalId) ? { ...a, categoria: "Vaca seca" } : a)));
      }
      setForm(novoEventoReproducao());
      setOpen(false);
    } finally {
      setSalvando(false);
    }
  };

  const remove = async (id) => {
    await reproducaoService.remover(id);
    setReproducao((prev) => prev.filter((e) => e.id !== id));
  };

  const eventos = [...reproducao].sort((a, b) => (a.data < b.data ? 1 : -1));

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.cio, color: "#fff" }}>
          <Plus size={16} /> Novo registro
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          <Field label="Animal *"><AnimalPicker animais={animais} value={form.animalId} onChange={(v) => setForm({ ...form, animalId: v })} /></Field>
          <Field label="Tipo de evento">
            <select className={inputCls} style={inputStyle} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {TIPOS_REPRO.map((t) => <option key={t.v} value={t.v}>{t.l}</option>)}
            </select>
          </Field>
          <Field label="Data"><input type="date" className={inputCls} style={inputStyle} value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></Field>
          {form.tipo === "inseminacao" && (
            <Field label="Touro / sêmen utilizado"><input className={inputCls} style={inputStyle} value={form.detalhe} onChange={(e) => setForm({ ...form, detalhe: e.target.value })} /></Field>
          )}
          {form.tipo === "diagnostico" && (
            <Field label="Resultado">
              <select className={inputCls} style={inputStyle} value={form.resultado} onChange={(e) => setForm({ ...form, resultado: e.target.value })}>
                <option value="positivo">Positivo (prenha)</option>
                <option value="negativo">Negativo (vazia)</option>
              </select>
            </Field>
          )}
          {form.tipo === "parto" && (
            <Field label="Observações (sexo da cria, ocorrências)"><input className={inputCls} style={inputStyle} value={form.detalhe} onChange={(e) => setForm({ ...form, detalhe: e.target.value })} /></Field>
          )}
          <div className="sm:col-span-2 flex justify-end gap-2">
            <button onClick={() => setOpen(false)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={add} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : "Salvar registro"}
            </button>
          </div>
        </div>
      )}

      {eventos.length === 0 ? (
        <EmptyState text="Nenhum evento reprodutivo registrado. Cios, inseminações e partos aparecem aqui." />
      ) : (
        <div className="flex flex-col gap-2">
          {eventos.map((e) => (
            <div key={e.id} className="rounded-xl border bg-white p-3 flex items-center gap-3" style={{ borderColor: C.line }}>
              <span className="text-xs rounded-md px-2 py-1 shrink-0" style={{ ...fMono, backgroundColor: C.pasto50, color: C.pasto900 }}>{formatDate(e.data)}</span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium" style={{ color: C.ink }}>{TIPOS_REPRO.find((t) => t.v === e.tipo)?.l} — {rotuloAnimal(animais.find((a) => a.id === e.animalId))}</p>
                {(e.detalhe || e.resultado) && (
                  <p className="text-xs" style={{ color: C.inkSoft }}>{e.tipo === "diagnostico" ? (e.resultado === "positivo" ? "Prenha" : "Vazia") : e.detalhe}</p>
                )}
              </div>
              <button onClick={() => remove(e.id)} className="p-1.5 rounded-md hover:bg-gray-100"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
