import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertTriangle, Loader2, RefreshCw, Upload } from "lucide-react";
import {
  Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { inputCls, inputStyle } from "../components/ui";
import { C, SERIE, fMono } from "../utils/tokens";
import { addDays, formatDate, todayStr } from "../utils/date";
import { fazendaService, indicadoresService } from "../services/indicadoresService";

const PERIODOS = [
  { v: 30, l: "30 dias" },
  { v: 90, l: "90 dias" },
  { v: 180, l: "6 meses" },
  { v: 365, l: "12 meses" },
  { v: "personalizado", l: "Personalizado" },
];

const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

const num = (v, casas = 1) =>
  v === null || v === undefined || Number.isNaN(v) ? "—" : Number(v).toLocaleString("pt-BR", { maximumFractionDigits: casas, minimumFractionDigits: 0 });
const diaCurto = (iso) => formatDate(iso).slice(0, 5);
const media = (valores) => (valores.length ? valores.reduce((s, v) => s + v, 0) / valores.length : null);

/** Carrega um recurso da API e guarda dados, erro e carregamento. */
function useRecurso(carregar, deps) {
  const [estado, setEstado] = useState({ dados: null, erro: "", carregando: true });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const executar = useCallback(carregar, deps);
  const recarregar = useCallback(async () => {
    setEstado((e) => ({ ...e, erro: "", carregando: true }));
    try {
      setEstado({ dados: await executar(), erro: "", carregando: false });
    } catch (err) {
      setEstado({ dados: null, erro: err.message || "Não foi possível carregar.", carregando: false });
    }
  }, [executar]);
  useEffect(() => {
    recarregar();
  }, [recarregar]);
  return { ...estado, recarregar };
}

export default function Indicadores() {
  const [periodo, setPeriodo] = useState(90);
  const [personalizado, setPersonalizado] = useState({ inicio: addDays(todayStr(), -89), fim: todayStr() });
  const { inicio, fim } = periodo === "personalizado"
    ? personalizado
    : { inicio: addDays(todayStr(), -(periodo - 1)), fim: todayStr() };

  const serie = useRecurso(() => indicadoresService.serie(inicio, fim), [inicio, fim]);
  const clima = useRecurso(() => indicadoresService.clima(inicio, fim), [inicio, fim]);
  const repro = useRecurso(() => indicadoresService.reproducao(), []);
  const projecao = useRecurso(() => indicadoresService.projecao(), []);
  const fazenda = useRecurso(() => fazendaService.atual(), []);

  return (
    <div className="flex flex-col gap-5">
      {/* filtros: uma linha acima dos gráficos */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold" style={{ color: C.ink }}>Período</span>
          <div className="flex rounded-lg border overflow-hidden" style={{ borderColor: C.line }} role="group" aria-label="Período">
            {PERIODOS.map((p) => (
              <button
                key={p.v}
                onClick={() => setPeriodo(p.v)}
                aria-pressed={periodo === p.v}
                className="px-3 py-1.5 text-sm"
                style={{ backgroundColor: periodo === p.v ? C.pasto900 : "#fff", color: periodo === p.v ? "#fff" : C.inkSoft }}
              >
                {p.l}
              </button>
            ))}
          </div>
          {periodo === "personalizado" && (
            <div className="flex items-center gap-1.5">
              <input type="date" className={inputCls} style={inputStyle} value={personalizado.inicio} max={personalizado.fim}
                onChange={(e) => e.target.value && setPersonalizado({ ...personalizado, inicio: e.target.value })} aria-label="Início" />
              <span className="text-sm" style={{ color: C.inkSoft }}>até</span>
              <input type="date" className={inputCls} style={inputStyle} value={personalizado.fim} max={todayStr()}
                onChange={(e) => e.target.value && setPersonalizado({ ...personalizado, fim: e.target.value })} aria-label="Fim" />
            </div>
          )}
        </div>
        <span className="text-xs" style={{ ...fMono, color: C.inkSoft }}>{formatDate(inicio)} – {formatDate(fim)}</span>
      </div>

      <SecaoRebanho serie={serie} />
      <SecaoClima clima={clima} fazenda={fazenda} />
      <SecaoReproducao repro={repro} />
      <SecaoProjecao projecao={projecao} />
    </div>
  );
}

// ------------------------------------------------------------------ produção e lactação

function SecaoRebanho({ serie }) {
  const resumo = useMemo(() => {
    const dias = serie.dados || [];
    const comLitros = dias.filter((d) => d.litrosTotal !== null);
    const porVaca = dias.filter((d) => d.litrosPorVacaEmLactacao !== null).map((d) => d.litrosPorVacaEmLactacao);
    const ultimo = dias[dias.length - 1];
    return {
      ultimo,
      producaoMedia: media(comLitros.map((d) => Number(d.litrosTotal))),
      diasComLitros: comLitros.length,
      diasEstimados: comLitros.filter((d) => d.estimado).length,
      porVacaMedia: media(porVaca),
      diasPorVaca: porVaca.length,
      misturados: dias.filter((d) => d.misturado).length,
      grafico: dias.map((d) => ({
        dia: diaCurto(d.data),
        data: d.data,
        medido: d.litrosTotal !== null && !d.estimado ? Number(d.litrosTotal) : null,
        estimado: d.litrosTotal !== null && d.estimado ? Number(d.litrosTotal) : null,
        porVaca: d.litrosPorVacaEmLactacao,
        vacas: d.vacasEmLactacao,
      })),
    };
  }, [serie.dados]);

  return (
    <Secao titulo="Produção e lactação" recurso={serie}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Cartao titulo="Vacas em lactação" valor={num(resumo.ultimo?.vacasEmLactacao, 0)}
          detalhe={resumo.ultimo ? `em ${formatDate(resumo.ultimo.data)}` : ""} />
        <Cartao titulo="DEL médio" valor={num(resumo.ultimo?.delMedio)} unidade="dias"
          casos={resumo.ultimo?.vacasEmLactacao} rotuloCasos="vacas" />
        <Cartao titulo="Produção média por dia" valor={num(resumo.producaoMedia)} unidade="L"
          casos={resumo.diasComLitros} rotuloCasos="dias com registro"
          detalhe={resumo.diasEstimados ? `${resumo.diasEstimados} dia(s) estimados` : ""} />
        <Cartao titulo="Litros por vaca em lactação" valor={num(resumo.porVacaMedia)} unidade="L/vaca"
          casos={resumo.diasPorVaca} rotuloCasos="dias" />
      </div>

      {resumo.misturados > 0 && (
        <Aviso texto={`${resumo.misturados} dia(s) antigos têm ordenhas por animal e total do rebanho juntos; nesses dias o total usa só os registros por animal.`} />
      )}

      {resumo.diasComLitros === 0 ? (
        <SemDados texto="Nenhuma ordenha registrada no período. Registre ou importe a produção na aba Ordenha." />
      ) : (
        <div className="grid lg:grid-cols-2 gap-3">
          <Grafico titulo="Produção diária do rebanho (litros)">
            <BarChart data={resumo.grafico} barCategoryGap={1}>
              <CartesianGrid stroke={SERIE.grade} vertical={false} />
              <XAxis dataKey="dia" tick={eixo} tickLine={false} axisLine={{ stroke: SERIE.grade }} minTickGap={16} />
              <YAxis tick={eixo} tickLine={false} axisLine={false} width={44} />
              <Tooltip content={<Dica unidade="L" />} cursor={{ fill: C.pasto50 }} />
              <Legend wrapperStyle={legenda} />
              <Bar dataKey="medido" name="Medido" stackId="l" fill={SERIE.s1} radius={[4, 4, 0, 0]} />
              <Bar dataKey="estimado" name="Estimado (coleta de vários dias)" stackId="l" fill={SERIE.s2} radius={[4, 4, 0, 0]} />
            </BarChart>
          </Grafico>
          <Grafico titulo="Litros por vaca em lactação">
            {resumo.diasPorVaca === 0 ? (
              <SemDados texto="Sem vacas em lactação registradas no período (registre os partos na aba Reprodução)." />
            ) : (
              <LineChart data={resumo.grafico}>
                <CartesianGrid stroke={SERIE.grade} vertical={false} />
                <XAxis dataKey="dia" tick={eixo} tickLine={false} axisLine={{ stroke: SERIE.grade }} minTickGap={16} />
                <YAxis tick={eixo} tickLine={false} axisLine={false} width={44} />
                <Tooltip content={<Dica unidade="L/vaca" />} />
                <Line dataKey="porVaca" name="Litros por vaca" stroke={SERIE.s1} strokeWidth={2} dot={false} connectNulls={false}
                  activeDot={{ r: 5, stroke: "#fff", strokeWidth: 2 }} />
              </LineChart>
            )}
          </Grafico>
        </div>
      )}
    </Secao>
  );
}

// ------------------------------------------------------------------ clima

function SecaoClima({ clima, fazenda }) {
  const [importando, setImportando] = useState(false);
  const [resultado, setResultado] = useState("");
  const [erroImport, setErroImport] = useState("");
  const estacao = fazenda.dados?.estacaoInmet;

  const importar = async (e) => {
    const arquivo = e.target.files?.[0];
    e.target.value = "";
    if (!arquivo) return;
    setImportando(true);
    setErroImport("");
    setResultado("");
    try {
      const r = await indicadoresService.importarClima(arquivo);
      setResultado(`Estação ${r.estacao}: ${r.importados} dia(s) novos e ${r.substituidos} substituído(s), de ${formatDate(r.primeiroDia)} a ${formatDate(r.ultimoDia)}.`);
      clima.recarregar();
    } catch (err) {
      setErroImport(err.message || "Não foi possível importar o arquivo.");
    } finally {
      setImportando(false);
    }
  };

  const dias = clima.dados || [];
  const ituMedio = media(dias.filter((d) => d.ituMedio !== null).map((d) => d.ituMedio));
  const comMax = dias.filter((d) => d.ituMax !== null);
  const diasEstresse = comMax.filter((d) => d.ituMax > 72).length;
  const grafico = dias.map((d) => ({ dia: diaCurto(d.data), data: d.data, medio: d.ituMedio, maximo: d.ituMax }));

  const acoes = (
    <label className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-semibold cursor-pointer"
      style={{ backgroundColor: estacao ? C.pasto900 : C.line, color: estacao ? "#fff" : C.inkSoft }}
      title={estacao ? "Arquivo horário do INMET ou CSV diário" : "Configure a estação do INMET primeiro"}>
      {importando ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
      Importar CSV de clima
      <input type="file" accept=".csv,text/csv,text/plain" className="hidden" disabled={!estacao || importando} onChange={importar} />
    </label>
  );

  return (
    <Secao titulo="Estresse térmico (ITU)" recurso={clima} acoes={acoes}>
      {erroImport && <Aviso texto={erroImport} erro />}
      {resultado && <p className="text-sm" style={{ color: C.pasto900 }}>{resultado}</p>}
      {!fazenda.carregando && !estacao ? (
        <SemDados texto="Informe a estação do INMET da fazenda nas configurações (engrenagem no topo) para buscar o clima e calcular o ITU." />
      ) : dias.length === 0 ? (
        <SemDados texto={`Sem dados de clima da estação ${estacao || ""} no período. A busca automática precisa do token do INMET; sem ele, importe o CSV histórico da estação.`} />
      ) : (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <Cartao titulo="ITU médio no período" valor={num(ituMedio)} casos={dias.length} rotuloCasos="dias com clima" />
            <Cartao titulo="Dias com ITU máximo acima de 72" valor={num(diasEstresse, 0)} casos={comMax.length} rotuloCasos="dias" />
          </div>
          <Grafico titulo={`ITU diário — estação ${estacao}`}>
            <LineChart data={grafico}>
              <CartesianGrid stroke={SERIE.grade} vertical={false} />
              <XAxis dataKey="dia" tick={eixo} tickLine={false} axisLine={{ stroke: SERIE.grade }} minTickGap={16} />
              <YAxis tick={eixo} tickLine={false} axisLine={false} width={36} domain={["dataMin - 2", "dataMax + 2"]} />
              <Tooltip content={<Dica casas={1} />} />
              <Legend wrapperStyle={legenda} />
              <ReferenceLine y={72} stroke={SERIE.referencia} strokeDasharray="4 4"
                label={{ value: "72 · início do estresse", position: "insideTopLeft", fill: C.inkSoft, fontSize: 11 }} />
              <Line dataKey="medio" name="ITU médio" stroke={SERIE.s1} strokeWidth={2} dot={false} activeDot={{ r: 5, stroke: "#fff", strokeWidth: 2 }} />
              <Line dataKey="maximo" name="ITU máximo" stroke={SERIE.s2} strokeWidth={2} dot={false} activeDot={{ r: 5, stroke: "#fff", strokeWidth: 2 }} />
            </LineChart>
          </Grafico>
        </>
      )}
    </Secao>
  );
}

// ------------------------------------------------------------------ reprodução

function SecaoReproducao({ repro }) {
  const r = repro.dados;
  return (
    <Secao titulo={r ? `Eficiência reprodutiva — ${formatDate(r.inicio)} a ${formatDate(r.fim)}` : "Eficiência reprodutiva — últimos 12 meses"} recurso={repro}>
      {r && (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            <Cartao titulo="Intervalo entre partos (média)" valor={num(r.iepMedio.valor, 0)} unidade="dias"
              casos={r.iepMedio.casos} rotuloCasos="intervalos" />
            <Cartao titulo="Dias em aberto (média)" valor={num(r.diasEmAberto.valor, 0)} unidade="dias"
              casos={r.diasEmAberto.casos} rotuloCasos="prenhezes" detalhe="do parto à cobertura que gerou prenhez" />
            <Cartao titulo="Taxa de concepção" valor={num(r.taxaConcepcao.valor)} unidade="%"
              casos={r.taxaConcepcao.casos} rotuloCasos="coberturas com resultado" />
            <Cartao titulo="Perdas gestacionais" valor={num(r.perdasGestacionais.quantidade, 0)}
              detalhe={r.perdasGestacionais.percentual !== null ? `${num(r.perdasGestacionais.percentual)}% das prenhezes confirmadas` : "sem prenhezes confirmadas"}
              casos={r.perdasGestacionais.prenhezesConfirmadas} rotuloCasos="prenhezes confirmadas" />
            <Cartao titulo={`Vacas vazias há mais de ${r.diasVaziaAlerta} dias pós-parto`} valor={num(r.vacasVazias.length, 0)}
              detalhe={r.vacasVazias.slice(0, 6).map((v) => `#${v.brinco} (${v.diasPosParto}d)`).join(", ") + (r.vacasVazias.length > 6 ? "…" : "")} />
          </div>
          {r.iepPorVaca.length > 0 && (
            <details className="rounded-xl border bg-white p-3 text-sm" style={{ borderColor: C.line }}>
              <summary className="cursor-pointer font-semibold" style={{ color: C.ink }}>Intervalo entre partos por vaca ({r.iepPorVaca.length})</summary>
              <table className="mt-2 w-full text-left">
                <thead><tr style={{ color: C.inkSoft }}><th className="py-1 font-medium">Vaca</th><th className="font-medium text-right">Média (dias)</th><th className="font-medium text-right">Intervalos</th></tr></thead>
                <tbody>
                  {r.iepPorVaca.map((v) => (
                    <tr key={v.animalId} className="border-t" style={{ borderColor: C.line }}>
                      <td className="py-1">#{v.brinco}{v.nome ? ` · ${v.nome}` : ""}</td>
                      <td className="text-right" style={fMono}>{num(v.mediaDias, 0)}</td>
                      <td className="text-right" style={fMono}>{v.intervalos}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </details>
          )}
        </>
      )}
    </Secao>
  );
}

// ------------------------------------------------------------------ projeção

function SecaoProjecao({ projecao }) {
  const meses = (projecao.dados || []).map((m) => {
    const [ano, mes] = m.mes.split("-").map(Number);
    return { ...m, rotulo: `${MESES[mes - 1]}/${String(ano).slice(2)}` };
  });
  const semDados = meses.every((m) => m.emLactacao === 0 && m.semPrevisao === 0);

  return (
    <Secao titulo="Projeção de vacas em lactação — próximos 12 meses" recurso={projecao}>
      {semDados ? (
        <SemDados texto="Sem vacas em lactação ou prenhes para projetar. Registre partos, coberturas e diagnósticos na aba Reprodução." />
      ) : (
        <>
          <Grafico titulo="Vacas em lactação no fim de cada mês">
            <BarChart data={meses} barCategoryGap="20%">
              <CartesianGrid stroke={SERIE.grade} vertical={false} />
              <XAxis dataKey="rotulo" tick={eixo} tickLine={false} axisLine={{ stroke: SERIE.grade }} />
              <YAxis tick={eixo} tickLine={false} axisLine={false} width={36} allowDecimals={false} />
              <Tooltip content={<Dica unidade="vacas" casas={0} />} cursor={{ fill: C.pasto50 }} />
              <Legend wrapperStyle={legenda} />
              <Bar dataKey="emLactacao" name="Em lactação (previsto)" stackId="v" fill={SERIE.s1} stroke="#fff" strokeWidth={2} />
              <Bar dataKey="semPrevisao" name="Lactantes sem prenhez (sem previsão)" stackId="v" fill={SERIE.s2} stroke="#fff" strokeWidth={2} radius={[4, 4, 0, 0]} />
            </BarChart>
          </Grafico>
          <details className="rounded-xl border bg-white p-3 text-sm" style={{ borderColor: C.line }}>
            <summary className="cursor-pointer font-semibold" style={{ color: C.ink }}>Tabela da projeção</summary>
            <table className="mt-2 w-full text-left">
              <thead>
                <tr style={{ color: C.inkSoft }}>
                  <th className="py-1 font-medium">Mês</th><th className="font-medium text-right">Em lactação</th>
                  <th className="font-medium text-right">Secagens</th><th className="font-medium text-right">Partos</th>
                  <th className="font-medium text-right">Sem previsão</th>
                </tr>
              </thead>
              <tbody>
                {meses.map((m) => (
                  <tr key={m.mes} className="border-t" style={{ borderColor: C.line, ...fMono }}>
                    <td className="py-1" style={{ fontFamily: "inherit" }}>{m.rotulo}</td>
                    <td className="text-right">{m.emLactacao}</td><td className="text-right">{m.secagens}</td>
                    <td className="text-right">{m.partos}</td><td className="text-right">{m.semPrevisao}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>
        </>
      )}
    </Secao>
  );
}

// ------------------------------------------------------------------ peças

const eixo = { fontSize: 11, fill: C.inkSoft };
const legenda = { fontSize: 12, color: C.inkSoft, paddingTop: 4 };

/** Seção com estados de carregamento e erro: nunca mostra um gráfico vazio quando a API falha. */
function Secao({ titulo, recurso, acoes, children }) {
  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-base font-semibold" style={{ color: C.pasto900 }}>{titulo}</h2>
        {acoes}
      </div>
      {recurso.carregando && !recurso.dados ? (
        <div className="flex items-center gap-2 text-sm py-6" style={{ color: C.inkSoft }}>
          <Loader2 size={16} className="animate-spin" /> Carregando…
        </div>
      ) : recurso.erro ? (
        <div className="rounded-xl border p-4 flex flex-wrap items-center gap-3" style={{ borderColor: C.cio + "66", backgroundColor: C.cio + "0d" }} role="alert">
          <AlertTriangle size={18} color={C.cio} />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold" style={{ color: C.cio }}>Não foi possível carregar estes dados.</p>
            <p className="text-sm" style={{ color: C.ink }}>{recurso.erro}</p>
          </div>
          <button onClick={recurso.recarregar} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
            <RefreshCw size={14} /> Tentar de novo
          </button>
        </div>
      ) : (
        children
      )}
    </section>
  );
}

function Cartao({ titulo, valor, unidade, casos, rotuloCasos, detalhe }) {
  return (
    <div className="rounded-xl border bg-white p-3 flex flex-col gap-1" style={{ borderColor: C.line }}>
      <p className="text-xs font-medium" style={{ color: C.inkSoft }}>{titulo}</p>
      <p style={{ ...fMono, color: C.ink }} className="text-2xl font-semibold leading-tight">
        {valor}{unidade && valor !== "—" && <span className="text-sm font-normal ml-1" style={{ color: C.inkSoft }}>{unidade}</span>}
      </p>
      {detalhe && <p className="text-xs" style={{ color: C.inkSoft }}>{detalhe}</p>}
      {casos !== undefined && casos !== null && (
        <p className="text-[11px] mt-auto" style={{ color: C.inkSoft }}>n = {casos} {rotuloCasos}</p>
      )}
    </div>
  );
}

function Grafico({ titulo, children }) {
  return (
    <div className="rounded-xl border bg-white p-3" style={{ borderColor: C.line }}>
      <h3 className="text-sm font-semibold mb-2" style={{ color: C.ink }}>{titulo}</h3>
      <div style={{ height: 240 }}>
        <ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer>
      </div>
    </div>
  );
}

function Dica({ active, payload, label, unidade = "", casas = 1 }) {
  if (!active || !payload?.length) return null;
  const itens = payload.filter((p) => p.value !== null && p.value !== undefined);
  if (!itens.length) return null;
  return (
    <div className="rounded-lg border bg-white px-3 py-2 text-xs shadow-sm" style={{ borderColor: C.line }}>
      <p className="font-semibold mb-1" style={{ color: C.ink }}>{label}</p>
      {itens.map((p) => (
        <p key={p.dataKey} className="flex items-center gap-1.5" style={{ color: C.ink }}>
          <span className="inline-block w-2 h-2 rounded-full" style={{ backgroundColor: p.color }} />
          <span style={{ color: C.inkSoft }}>{p.name}:</span>
          <span style={fMono}>{num(p.value, casas)} {unidade}</span>
        </p>
      ))}
    </div>
  );
}

function SemDados({ texto }) {
  return (
    <div className="rounded-xl border border-dashed p-5 text-sm text-center" style={{ borderColor: C.line, color: C.inkSoft }}>
      {texto}
    </div>
  );
}

function Aviso({ texto, erro = false }) {
  return (
    <p className="text-xs rounded-lg px-3 py-2 flex items-start gap-1.5"
      style={{ color: erro ? C.cio : C.brincoDark, backgroundColor: (erro ? C.cio : C.brinco) + "14" }} role={erro ? "alert" : undefined}>
      <AlertTriangle size={14} className="shrink-0 mt-0.5" /> {texto}
    </p>
  );
}
