import { useMemo, useState } from "react";
import { Plus, TrendingUp, Trash2, Upload, Loader2 } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { Field, EmptyState, ErrorText, inputCls, inputStyle } from "../components/ui";
import AnimalPicker from "../components/AnimalPicker";
import { C, fMono } from "../utils/tokens";
import { addDays, formatDate, todayStr } from "../utils/date";
import { novaOrdenha } from "../models/Ordenha";
import { rotuloAnimal } from "../models/Animal";
import { ordenhaService } from "../services/ordenhaService";
import { litrosPorDia } from "../utils/producao";

export default function Ordenha({ data }) {
  const { animais, ordenha, setOrdenha } = data;
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(novaOrdenha());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  const add = async () => {
    if (!form.litros) return;
    setSalvando(true);
    setErro("");
    try {
      const dados = { ...form, diasColeta: form.animalId ? 1 : Number(form.diasColeta) || 1 };
      const criados = await ordenhaService.criar(dados, form.animalId || undefined);
      setOrdenha((prev) => [...prev, ...criados]);
      setForm(novaOrdenha());
      setOpen(false);
    } catch (e) {
      setErro(e.message || "Não foi possível salvar a ordenha.");
    } finally {
      setSalvando(false);
    }
  };

  const [importando, setImportando] = useState(false);
  const [resumo, setResumo] = useState(null);

  const importar = async (e) => {
    const arquivo = e.target.files?.[0];
    e.target.value = "";
    if (!arquivo) return;
    setImportando(true);
    setErro("");
    setResumo(null);
    try {
      const r = await ordenhaService.importar(arquivo);
      setResumo(r);
      // recarrega só as ordenhas (recarregar tudo trocaria a tela pelo "carregando" e sumiria com o resumo)
      if (r.importadas + r.substituidas > 0) setOrdenha(await ordenhaService.listarPorPeriodo(addDays(todayStr(), -60), todayStr()));
    } catch (err) {
      setErro(err.message || "Não foi possível importar o arquivo.");
    } finally {
      setImportando(false);
    }
  };

  const remove = async (registro) => {
    const msg = registro.coletaId
      ? `Este registro faz parte de uma coleta de ${registro.diasColeta} dias. Remover a coleta inteira?`
      : "Remover este registro de ordenha?";
    if (!window.confirm(msg)) return;
    setErro("");
    try {
      await ordenhaService.remover(registro.id);
      setOrdenha((prev) => prev.filter((e) => e.id !== registro.id && !(registro.coletaId && e.coletaId === registro.coletaId)));
    } catch (e) {
      setErro(e.message || "Não foi possível remover o registro.");
    }
  };

  const chartData = useMemo(() => {
    const porDia = litrosPorDia(ordenha);
    const dias = [];
    for (let i = 13; i >= 0; i--) dias.push(addDays(todayStr(), -i));
    return dias.map((d) => ({ data: formatDate(d).slice(0, 5), litros: porDia[d] || 0 }));
  }, [ordenha]);

  const registros = [...ordenha].sort((a, b) => (a.data < b.data ? 1 : -1)).slice(0, 30);

  return (
    <div className="flex flex-col gap-4">
      <ErrorText text={erro} />
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

      {resumo && (
        <div className="rounded-xl border bg-white p-4 text-sm" style={{ borderColor: C.line }}>
          <p style={{ color: C.ink }}>
            Importação concluída: <strong>{resumo.importadas}</strong> dia(s) novos, <strong>{resumo.substituidas}</strong> substituído(s)
            e <strong>{resumo.rejeitadas.length}</strong> linha(s) rejeitada(s).
          </p>
          {resumo.rejeitadas.length > 0 && (
            <ul className="mt-2 text-xs flex flex-col gap-0.5 max-h-48 overflow-y-auto" style={{ color: C.cio }}>
              {resumo.rejeitadas.map((r) => (
                <li key={r.linha}>Linha {r.linha} <span style={{ ...fMono, color: C.inkSoft }}>({r.conteudo})</span>: {r.motivo}</li>
              ))}
            </ul>
          )}
          <button onClick={() => setResumo(null)} className="mt-2 text-xs underline" style={{ color: C.inkSoft }}>Fechar</button>
        </div>
      )}

      <div className="flex flex-wrap justify-end gap-2">
        <label className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold cursor-pointer border" style={{ borderColor: C.line, color: C.pasto900, backgroundColor: "#fff" }}
          title="CSV com duas colunas: data;litros (total do rebanho por dia)">
          {importando ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />}
          Importar CSV (data;litros)
          <input type="file" accept=".csv,text/csv,text/plain" className="hidden" disabled={importando} onChange={importar} />
        </label>
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
          {!form.animalId && (
            <div className="sm:col-span-2">
              <Field label="Coleta de quantos dias?">
                <input type="number" min="1" max="31" className={inputCls} style={inputStyle} value={form.diasColeta} onChange={(e) => setForm({ ...form, diasColeta: e.target.value })} />
              </Field>
              <p className="text-xs mt-1" style={{ color: C.inkSoft }}>
                Deixe 1 se o volume é deste dia. Se o leite foi coletado a cada N dias, informe N: a data acima é o último dia, e o volume é dividido igualmente entre os N dias (marcados como estimados).
              </p>
            </div>
          )}
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
                  {r.pendente && <span className="ml-2 text-xs px-1.5 py-0.5 rounded" style={{ backgroundColor: C.pasto50, color: C.inkSoft, border: `1px dashed ${C.line}` }}>aguardando envio</span>}
                  {r.estimado && <span className="ml-2 text-xs px-1.5 py-0.5 rounded" style={{ backgroundColor: C.brinco + "22", color: C.brincoDark }}>estimado · coleta de {r.diasColeta} dias</span>}
                </p>
              </div>
              <span style={{ ...fMono, color: C.leite, fontWeight: 700 }} className="text-sm">{r.litros} L</span>
              <button onClick={() => remove(r)} className="p-1.5 rounded-md hover:bg-gray-100"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
