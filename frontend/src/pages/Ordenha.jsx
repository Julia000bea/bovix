import { useMemo, useState } from "react";
import { Plus, TrendingUp, Trash2 } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { Field, EmptyState, inputCls, inputStyle } from "../components/ui";
import AnimalPicker from "../components/AnimalPicker";
import { C, fMono } from "../utils/tokens";
import { addDays, formatDate, todayStr } from "../utils/date";
import { novaOrdenha } from "../models/Ordenha";
import { rotuloAnimal } from "../models/Animal";
import { ordenhaService } from "../services/ordenhaService";

export default function Ordenha({ data }) {
  const { animais, ordenha, setOrdenha } = data;
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(novaOrdenha());
  const [salvando, setSalvando] = useState(false);

  const add = async () => {
    if (!form.litros) return;
    setSalvando(true);
    try {
      const criado = await ordenhaService.criar(form, form.animalId || undefined);
      setOrdenha((prev) => [...prev, criado]);
      setForm(novaOrdenha());
      setOpen(false);
    } finally {
      setSalvando(false);
    }
  };

  const remove = async (id) => {
    await ordenhaService.remover(id);
    setOrdenha((prev) => prev.filter((e) => e.id !== id));
  };

  const chartData = useMemo(() => {
    const map = {};
    for (let i = 13; i >= 0; i--) map[addDays(todayStr(), -i)] = 0;
    for (const o of ordenha) if (map[o.data] !== undefined) map[o.data] += Number(o.litros || 0);
    return Object.entries(map).map(([d, litros]) => ({ data: formatDate(d).slice(0, 5), litros }));
  }, [ordenha]);

  const registros = [...ordenha].sort((a, b) => (a.data < b.data ? 1 : -1)).slice(0, 30);

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-xl border bg-white p-4" style={{ borderColor: C.line }}>
        <div className="flex items-center gap-2 mb-2">
          <TrendingUp size={16} color={C.leite} />
          <h3 className="text-sm font-semibold" style={{ color: C.ink }}>Produção do rebanho — últimos 14 dias (litros)</h3>
        </div>
        {ordenha.length === 0 ? (
          <EmptyState text="Registre a ordenha para ver o gráfico de produção." />
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={chartData}>
              <CartesianGrid stroke={C.line} strokeDasharray="3 3" />
              <XAxis dataKey="data" tick={{ fontSize: 11, fill: C.inkSoft }} />
              <YAxis tick={{ fontSize: 11, fill: C.inkSoft }} />
              <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: C.line }} />
              <Line type="monotone" dataKey="litros" stroke={C.leite} strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      <div className="flex justify-end">
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.brinco, color: "#fff" }}>
          <Plus size={16} /> Registrar ordenha
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          <Field label="Animal (opcional — deixe vazio para total do rebanho)"><AnimalPicker animais={animais} value={form.animalId} onChange={(v) => setForm({ ...form, animalId: v })} /></Field>
          <Field label="Data"><input type="date" className={inputCls} style={inputStyle} value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></Field>
          <Field label="Período">
            <select className={inputCls} style={inputStyle} value={form.periodo} onChange={(e) => setForm({ ...form, periodo: e.target.value })}>
              <option>Manhã</option><option>Tarde</option><option>Dia todo</option>
            </select>
          </Field>
          <Field label="Litros *"><input type="number" min="0" step="0.1" className={inputCls} style={inputStyle} value={form.litros} onChange={(e) => setForm({ ...form, litros: e.target.value })} /></Field>
          <div className="sm:col-span-2 flex justify-end gap-2">
            <button onClick={() => setOpen(false)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={add} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : "Salvar registro"}
            </button>
          </div>
        </div>
      )}

      {registros.length === 0 ? (
        <EmptyState text="Nenhum registro de ordenha ainda." />
      ) : (
        <div className="flex flex-col gap-2">
          {registros.map((r) => (
            <div key={r.id} className="rounded-xl border bg-white p-3 flex items-center gap-3" style={{ borderColor: C.line }}>
              <span className="text-xs rounded-md px-2 py-1 shrink-0" style={{ ...fMono, backgroundColor: C.pasto50, color: C.pasto900 }}>{formatDate(r.data)}</span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium" style={{ color: C.ink }}>
                  {r.animalId ? rotuloAnimal(animais.find((a) => a.id === r.animalId)) : "Total do rebanho"} · {r.periodo}
                </p>
              </div>
              <span style={{ ...fMono, color: C.leite, fontWeight: 700 }} className="text-sm">{r.litros} L</span>
              <button onClick={() => remove(r.id)} className="p-1.5 rounded-md hover:bg-gray-100"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
