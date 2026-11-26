import { useState } from "react";
import { Plus, Wallet, Trash2 } from "lucide-react";
import { Field, Pill, EmptyState, ErrorText, inputCls, inputStyle } from "../components/ui";
import { C, CATEGORIAS_GASTO, CATEGORIAS_RECEITA, ehDespesa, fMono } from "../utils/tokens";
import { formatDate, todayStr } from "../utils/date";
import { brl } from "../utils/format";
import { novoGasto } from "../models/Financeiro";
import { financeiroService } from "../services/financeiroService";

const corCategoria = {
  "Ração": C.brinco, "Insumos": C.leite, "Medicamentos": C.cio,
  "Mão de obra": C.terra, "Manutenção": C.pasto700, "Outros": C.inkSoft,
  "Venda de leite": C.pasto700,
};

export default function Financeiro({ data }) {
  const { financeiro, setFinanceiro } = data;
  const [open, setOpen] = useState(false);
  const [mesRef, setMesRef] = useState(todayStr().slice(0, 7));
  const [form, setForm] = useState(novoGasto());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  const add = async () => {
    if (!form.valor) return;
    setSalvando(true);
    setErro("");
    try {
      const criado = await financeiroService.criar(form);
      setFinanceiro((prev) => [...prev, criado]);
      setForm(novoGasto());
      setOpen(false);
    } catch (e) {
      setErro(e.message || "Não foi possível salvar o lançamento.");
    } finally {
      setSalvando(false);
    }
  };

  const remove = async (id) => {
    if (!window.confirm("Remover este lançamento?")) return;
    setErro("");
    try {
      await financeiroService.remover(id);
      setFinanceiro((prev) => prev.filter((e) => e.id !== id));
    } catch (e) {
      setErro(e.message || "Não foi possível remover o lançamento.");
    }
  };

  const doMes = financeiro.filter((g) => g.data.slice(0, 7) === mesRef);
  // totais e categorias contam só despesas; receitas aparecem à parte
  const gastosDoMes = doMes.filter(ehDespesa);
  const receitaMes = doMes.filter((g) => !ehDespesa(g)).reduce((s, g) => s + Number(g.valor || 0), 0);
  const totalMes = gastosDoMes.reduce((s, g) => s + Number(g.valor || 0), 0);
  const porCategoria = CATEGORIAS_GASTO.map((cat) => ({
    categoria: cat,
    total: gastosDoMes.filter((g) => g.categoria === cat).reduce((s, g) => s + Number(g.valor || 0), 0),
  })).filter((c) => c.total > 0);
  const registros = [...doMes].sort((a, b) => (a.data < b.data ? 1 : -1));

  return (
    <div className="flex flex-col gap-4">
      <ErrorText text={erro} />
      <div className="rounded-xl border bg-white p-4" style={{ borderColor: C.line }}>
        <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
          <div className="flex items-center gap-2">
            <Wallet size={16} color={C.brincoDark} />
            <h3 className="text-sm font-semibold" style={{ color: C.ink }}>Gastos do mês</h3>
          </div>
          <input type="month" className={inputCls} style={inputStyle} value={mesRef} onChange={(e) => setMesRef(e.target.value)} />
        </div>

        <p style={{ ...fMono, color: C.brincoDark }} className="text-3xl font-semibold">{brl(totalMes)}</p>
        <p className="text-xs mb-3" style={{ color: C.inkSoft }}>
          total gasto no mês selecionado
          {receitaMes > 0 && <> · receitas: <span style={{ ...fMono, color: C.pasto700, fontWeight: 600 }}>{brl(receitaMes)}</span></>}
        </p>

        {porCategoria.length === 0 ? (
          <EmptyState text="Nenhum gasto lançado neste mês ainda." />
        ) : (
          <div className="flex flex-col gap-2">
            {porCategoria.map((c) => (
              <div key={c.categoria} className="flex items-center gap-2">
                <span className="text-xs w-28 shrink-0" style={{ color: C.inkSoft }}>{c.categoria}</span>
                <div className="flex-1 h-2 rounded-full" style={{ backgroundColor: C.line }}>
                  <div className="h-2 rounded-full" style={{ width: `${Math.max(4, (c.total / totalMes) * 100)}%`, backgroundColor: corCategoria[c.categoria] }} />
                </div>
                <span style={{ ...fMono, color: C.ink }} className="text-xs w-24 text-right shrink-0">{brl(c.total)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-end">
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.brinco, color: "#fff" }}>
          <Plus size={16} /> Novo lançamento
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          <Field label="Tipo">
            <select className={inputCls} style={inputStyle} value={form.tipo} onChange={(e) => {
              const tipo = e.target.value;
              setForm({ ...form, tipo, categoria: tipo === "receita" ? CATEGORIAS_RECEITA[0] : CATEGORIAS_GASTO[0] });
            }}>
              <option value="despesa">Despesa</option>
              <option value="receita">Receita</option>
            </select>
          </Field>
          <Field label="Data"><input type="date" className={inputCls} style={inputStyle} value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></Field>
          <Field label="Categoria">
            <select className={inputCls} style={inputStyle} value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>
              {(form.tipo === "receita" ? CATEGORIAS_RECEITA : CATEGORIAS_GASTO).map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
          <div className="sm:col-span-2">
            <Field label="Descrição"><input className={inputCls} style={{ ...inputStyle, width: "100%" }} placeholder={form.tipo === "receita" ? "ex: leite da quinzena (1.850 L)" : "ex: 20 sacos de ração"} value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></Field>
          </div>
          <Field label="Valor (R$) *"><input type="number" min="0" step="0.01" className={inputCls} style={inputStyle} value={form.valor} onChange={(e) => setForm({ ...form, valor: e.target.value })} /></Field>
          <div className="flex items-end justify-end gap-2">
            <button onClick={() => setOpen(false)} className="px-3 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
            <button onClick={add} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
              {salvando ? "Salvando…" : form.tipo === "receita" ? "Salvar receita" : "Salvar gasto"}
            </button>
          </div>
        </div>
      )}

      {registros.length === 0 ? (
        <EmptyState text="Nenhum lançamento neste mês." />
      ) : (
        <div className="flex flex-col gap-2">
          {registros.map((g) => (
            <div key={g.id} className="rounded-xl border bg-white p-3 flex items-center gap-3" style={{ borderColor: C.line }}>
              <span className="text-xs rounded-md px-2 py-1 shrink-0" style={{ ...fMono, backgroundColor: C.pasto50, color: C.pasto900 }}>{formatDate(g.data)}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  {!ehDespesa(g) && <Pill color={C.pasto700}>Receita</Pill>}
                  <Pill color={corCategoria[g.categoria] || C.inkSoft}>{g.categoria}</Pill>
                  {g.descricao && <span className="text-sm" style={{ color: C.ink }}>{g.descricao}</span>}
                </div>
              </div>
              <span style={{ ...fMono, color: ehDespesa(g) ? C.brincoDark : C.pasto700, fontWeight: 700 }} className="text-sm">{ehDespesa(g) ? "" : "+ "}{brl(g.valor)}</span>
              <button onClick={() => remove(g.id)} className="p-1.5 rounded-md hover:bg-gray-100"><Trash2 size={16} color={C.cio} /></button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
