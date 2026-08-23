import { useState } from "react";
import { Plus, Search, Trash2 } from "lucide-react";
import { Field, Pill, EmptyState, inputCls, inputStyle } from "../components/ui";
import PhotoUpload from "../components/PhotoUpload";
import { C, CATEGORIAS, STATUS_ANIMAL, fMono } from "../utils/tokens";
import { formatDate } from "../utils/date";
import { novoAnimal } from "../models/Animal";
import { animaisService } from "../services/animaisService";

export default function Rebanho({ data }) {
  const { animais, setAnimais } = data;
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [form, setForm] = useState(novoAnimal());
  const [fotoPendente, setFotoPendente] = useState(null);
  const [salvando, setSalvando] = useState(false);

  const addAnimal = async () => {
    if (!form.brinco) return;
    setSalvando(true);
    try {
      let criado = await animaisService.criar(form);
      if (fotoPendente) {
        criado = await animaisService.enviarFoto(criado.id, fotoPendente);
      }
      setAnimais((prev) => [...prev, criado]);
      setForm(novoAnimal());
      setFotoPendente(null);
      setOpen(false);
    } finally {
      setSalvando(false);
    }
  };

  const updateStatus = async (id, status) => {
    const atual = animais.find((a) => a.id === id);
    const atualizado = await animaisService.atualizar(id, { ...atual, status });
    setAnimais((prev) => prev.map((a) => (a.id === id ? { ...a, status: atualizado.status } : a)));
  };

  const removeAnimal = async (id) => {
    await animaisService.remover(id);
    setAnimais((prev) => prev.filter((a) => a.id !== id));
  };

  const filtered = animais.filter(
    (a) => a.brinco.toLowerCase().includes(q.toLowerCase()) || (a.nome || "").toLowerCase().includes(q.toLowerCase())
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" color={C.inkSoft} />
          <input className={inputCls} style={{ ...inputStyle, paddingLeft: 32, width: "100%" }} placeholder="Buscar por brinco ou nome" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold shrink-0" style={{ backgroundColor: C.brinco, color: "#fff" }}>
          <Plus size={16} /> Novo animal
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 flex flex-col gap-3" style={{ borderColor: C.line }}>
          <PhotoUpload animalId={null} fotoUrl={null} onSelectFile={setFotoPendente} />
          <div className="grid sm:grid-cols-3 gap-3">
            <Field label="Brinco (nº) *"><input className={inputCls} style={inputStyle} value={form.brinco} onChange={(e) => setForm({ ...form, brinco: e.target.value })} /></Field>
            <Field label="Nome"><input className={inputCls} style={inputStyle} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} /></Field>
            <Field label="Raça"><input className={inputCls} style={inputStyle} value={form.raca} onChange={(e) => setForm({ ...form, raca: e.target.value })} /></Field>
            <Field label="Nascimento"><input type="date" className={inputCls} style={inputStyle} value={form.nascimento} onChange={(e) => setForm({ ...form, nascimento: e.target.value })} /></Field>
            <Field label="Categoria">
              <select className={inputCls} style={inputStyle} value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>
                {CATEGORIAS.map((c) => <option key={c}>{c}</option>)}
              </select>
            </Field>
            <Field label="Notas"><input className={inputCls} style={inputStyle} value={form.notas} onChange={(e) => setForm({ ...form, notas: e.target.value })} /></Field>
          </div>
          <div className="flex justify-end gap-2">
            <button onClick={() => setOpen(false)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={addAnimal} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : "Salvar animal"}
            </button>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState text="Nenhum animal cadastrado ainda. Toque em “Novo animal” para começar o rebanho." />
      ) : (
        <div className="flex flex-col gap-2">
          {filtered.map((a) => (
            <div key={a.id} className="rounded-xl border bg-white p-3 flex items-center gap-3" style={{ borderColor: C.line, opacity: a.status === "Ativa" ? 1 : 0.55 }}>
              <PhotoUpload
                animalId={a.id}
                fotoUrl={a.fotoUrl}
                size={48}
                onUploaded={(atualizado) => setAnimais((prev) => prev.map((x) => (x.id === a.id ? { ...x, fotoUrl: atualizado.fotoUrl } : x)))}
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span style={fMono} className="text-xs font-semibold" >#{a.brinco}</span>
                  <span className="font-semibold text-sm" style={{ color: C.ink }}>{a.nome || "Sem nome"}</span>
                  <Pill color={C.leite}>{a.categoria}</Pill>
                  {a.status !== "Ativa" && <Pill color={C.terra}>{a.status}</Pill>}
                </div>
                <p className="text-xs mt-0.5" style={{ color: C.inkSoft }}>
                  {a.raca || "raça não informada"} {a.nascimento && `· nasc. ${formatDate(a.nascimento)}`}
                </p>
              </div>
              <select value={a.status} onChange={(e) => updateStatus(a.id, e.target.value)} className="text-xs rounded-md border px-2 py-1.5" style={{ borderColor: C.line, color: C.inkSoft }}>
                {STATUS_ANIMAL.map((s) => <option key={s}>{s}</option>)}
              </select>
              <button onClick={() => removeAnimal(a.id)} className="p-1.5 rounded-md hover:bg-gray-100 shrink-0" title="Remover animal">
                <Trash2 size={16} color={C.cio} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
