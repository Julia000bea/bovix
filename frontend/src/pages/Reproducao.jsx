import { useState } from "react";
import { Plus, Trash2, CheckCircle, Circle, ChevronDown, ChevronRight, Search, Stethoscope, Loader2, X } from "lucide-react";
import { Field, EmptyState, ErrorText, inputCls, inputStyle } from "../components/ui";
import AnimalPicker from "../components/AnimalPicker";
import { C, TIPOS_REPRO, fMono } from "../utils/tokens";
import { addDays, formatDate, todayStr } from "../utils/date";
import { novoEventoReproducao } from "../models/EventoReproducao";
import { rotuloAnimal } from "../models/Animal";
import { reproducaoService } from "../services/reproducaoService";
import { animaisService } from "../services/animaisService";

const TIPOS_CATEGORIA = ["parto", "secagem", "pre_parto"];

export default function Reproducao({ data }) {
  const { animais, setAnimais, reproducao, setReproducao, config } = data;
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(novoEventoReproducao());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [salvoAgora, setSalvoAgora] = useState("");
  const [busca, setBusca] = useState("");
  const [abertos, setAbertos] = useState(() => new Set()); // ids das vacas com o histórico aberto // confirmação do "Salvar e continuar"
  const [registrandoReproducao, setRegistrandoReproducao] = useState(null);
  const [modalReproducao, setModalReproducao] = useState({
    aberta: false,
    cioId: null,
    animalId: null,
    tipoReproducao: "inseminacao", // "touro" ou "inseminacao"
    formReproducao: {
      dataReproducao: "",
      touroDados: "", // Touro utilizado
      observacoes: "",
      semenCodigo: "", // Código/nome do sêmen
    },
  });
  const gestacaoDias = Number(config?.gestacaoDias) || 283;
  const preverParto = (dataBase) => (dataBase ? addDays(dataBase, gestacaoDias) : null);

  // A categoria (lactante/seca) é decidida pelo backend a partir do evento mais recente;
  // depois de criar/excluir parto, secagem ou pré-parto, buscamos o animal atualizado.
  const sincronizarAnimal = async (animalId, tipo) => {
    if (!TIPOS_CATEGORIA.includes(tipo)) return;
    const atualizado = await animaisService.buscar(animalId);
    setAnimais((prev) => prev.map((a) => (a.id === atualizado.id ? atualizado : a)));
  };

  // continuar = true: mantém o formulário aberto com o mesmo animal e a mesma data
  const add = async (continuar = false) => {
    if (!form.animalId || !form.tipo) return;
    if (form.tipo === "diagnostico_prenhez" && !form.resultado) {
      setErro("Informe o resultado do diagnóstico: prenhe ou vazia.");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const criado = await reproducaoService.criar(form.animalId, {
        ...form,
        resultado: form.tipo === "diagnostico_prenhez" ? form.resultado : null,
        previsaoParto: form.tipo === "prenhez_confirmada" ? form.previsaoParto || null : null,
      });
      setReproducao((prev) => [...prev, criado]);
      // lançado sem internet: a categoria do animal é atualizada quando a fila for enviada
      if (!criado.pendente) await sincronizarAnimal(criado.animalId, criado.tipo);

      if (continuar) {
        const tipo = TIPOS_REPRO.find((t) => t.v === criado.tipo)?.l || criado.tipo;
        setSalvoAgora(`${tipo} de ${formatDate(criado.data)} salvo. Continue cadastrando para ${rotuloAnimal(animais.find((a) => a.id === criado.animalId))}.`);
        setForm({ ...novoEventoReproducao(), animalId: form.animalId, data: form.data, tipo: form.tipo });
      } else {
        setSalvoAgora("");
        setForm(novoEventoReproducao());
        setOpen(false);
      }
    } catch (e) {
      setSalvoAgora("");
      setErro(e.message || "Não foi possível salvar o evento.");
    } finally {
      setSalvando(false);
    }
  };

  const confirmarCio = async (cioId) => {
    setSalvando(true);
    setErro("");
    try {
      const atualizado = await reproducaoService.confirmarCio(cioId);
      setReproducao((prev) => prev.map((e) => (e.id === cioId ? atualizado : e)));
    } catch (e) {
      setErro(e.message || "Não foi possível confirmar o cio.");
    } finally {
      setSalvando(false);
    }
  };

  const abrirRegistroReproducao = (cioId, animalId) => {
    const cio = reproducao.find((e) => e.id === cioId);
    setModalReproducao({
      aberta: true,
      cioId,
      animalId,
      cioData: cio?.data,
      tipoReproducao: "inseminacao",
      formReproducao: {
        dataReproducao: "",
        touroDados: "",
        observacoes: "",
        semenCodigo: "",
      },
    });
  };

  const salvarReproducao = async () => {
    const { cioId, animalId, tipoReproducao, formReproducao } = modalReproducao;
    const { dataReproducao, touroDados, observacoes, semenCodigo } = formReproducao;

    if (!dataReproducao) {
      alert("Data da reprodução/inseminação é obrigatória");
      return;
    }

    if (tipoReproducao === "touro" && !touroDados) {
      alert("Touro utilizado é obrigatório");
      return;
    }

    if (tipoReproducao === "inseminacao" && !semenCodigo) {
      alert("Código/nome do sêmen é obrigatório");
      return;
    }

    setSalvando(true);
    setErro("");
    try {
      const novaReproducao = {
        animalId,
        tipo: tipoReproducao === "touro" ? "reproducao" : "inseminacao",
        data: dataReproducao,
        detalhe: tipoReproducao === "touro" ? [touroDados, observacoes].filter(Boolean).join(" · ") : semenCodigo,
        cioVinculadoId: cioId,
      };

      const criada = await reproducaoService.criar(animalId, novaReproducao);
      setReproducao((prev) => [...prev, criada]);
      setModalReproducao({ ...modalReproducao, aberta: false });
    } catch (e) {
      setErro(e.message || "Não foi possível registrar a reprodução.");
    } finally {
      setSalvando(false);
    }
  };

  const remove = async (evento) => {
    if (!window.confirm("Remover este evento reprodutivo?")) return;
    setErro("");
    try {
      await reproducaoService.remover(evento.id);
      // coberturas ligadas a este cio perdem o vínculo no banco (ON DELETE SET NULL)
      setReproducao((prev) => prev
        .filter((e) => e.id !== evento.id)
        .map((e) => (e.cioVinculadoId === evento.id ? { ...e, cioVinculadoId: null } : e)));
      await sincronizarAnimal(evento.animalId, evento.tipo);
    } catch (e) {
      setErro(e.message || "Não foi possível remover o evento.");
    }
  };

  const cioJaCoberto = (cioId) => reproducao.some((e) => e.cioVinculadoId === cioId);

  const agrupadosPorAnimal = animais.map((animal) => ({
    animal,
    eventos: reproducao.filter((e) => e.animalId === animal.id).sort((a, b) => new Date(b.data) - new Date(a.data) || b.id - a.id), // mesmo dia: o registrado por último primeiro
  }));

  const comEventos = agrupadosPorAnimal.filter((g) => g.eventos.length > 0);

  // ---- busca e cartões que abrem/fecham
  const termo = busca.trim().toLowerCase();
  const filtrados = comEventos.filter(({ animal }) =>
    !termo || String(animal.brinco).toLowerCase().includes(termo) || (animal.nome || "").toLowerCase().includes(termo));
  // com a busca apontando para uma vaca só, o histórico dela abre sozinho
  const estaAberto = (id) => abertos.has(id) || (termo !== "" && filtrados.length === 1);
  const alternar = (id) => setAbertos((prev) => {
    const novo = new Set(prev);
    if (novo.has(id)) novo.delete(id); else novo.add(id);
    return novo;
  });

  const registrarDiagnostico = async (animalId, resultado, dataDiagnostico) => {
    setErro("");
    const criado = await reproducaoService.criar(animalId, { tipo: "diagnostico_prenhez", data: dataDiagnostico, resultado });
    setReproducao((prev) => [...prev, criado]);
    return criado;
  };

  return (
    <div className="flex flex-col gap-4">
      {!modalReproducao.aberta && <ErrorText text={erro} />}
      <div className="flex justify-end">
        <button
          onClick={() => {
            setRegistrandoReproducao(null);
            setSalvoAgora("");
            setOpen((o) => !o);
          }}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold"
          style={{ backgroundColor: C.cio, color: "#fff" }}
        >
          <Plus size={16} /> Novo evento
        </button>
      </div>

      {open && (
        <div className="rounded-xl border bg-white p-4 grid sm:grid-cols-2 gap-3" style={{ borderColor: C.line }}>
          {!registrandoReproducao && (
            <Field label="Animal *">
              <AnimalPicker animais={animais} value={form.animalId} onChange={(v) => setForm({ ...form, animalId: v })} />
            </Field>
          )}
          {registrandoReproducao && (
            <Field label="Animal">
              <input
                className={inputCls}
                style={inputStyle}
                value={rotuloAnimal(animais.find((a) => a.id === form.animalId))}
                disabled
              />
            </Field>
          )}

          <Field label="Tipo de evento">
            <select className={inputCls} style={inputStyle} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {TIPOS_REPRO.map((t) => (
                <option key={t.v} value={t.v}>
                  {t.l}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Data">
            <input
              type="date"
              className={inputCls}
              style={inputStyle}
              value={form.data}
              max={todayStr()}
              onChange={(e) => setForm({ ...form, data: e.target.value })}
            />
          </Field>

          {["inseminacao", "reproducao"].includes(form.tipo) && (
            <Field label={form.tipo === "inseminacao" ? "Touro / Sêmen utilizado" : "Touro utilizado"}>
              <input className={inputCls} style={inputStyle} value={form.detalhe} onChange={(e) => setForm({ ...form, detalhe: e.target.value })} />
            </Field>
          )}

          {["inseminacao", "reproducao"].includes(form.tipo) && <PrevisaoParto data={preverParto(form.data)} dias={gestacaoDias} />}

          {form.tipo === "diagnostico_prenhez" && (
            <Field label="Resultado do diagnóstico *">
              <select className={inputCls} style={inputStyle} value={form.resultado} onChange={(e) => setForm({ ...form, resultado: e.target.value })}>
                <option value="">Selecione...</option>
                <option value="prenhe">Prenhe</option>
                <option value="vazia">Vazia (não prenha)</option>
              </select>
            </Field>
          )}

          {form.tipo === "prenhez_confirmada" && (
            <Field label={`Data prevista do parto (calculada automaticamente: cobertura + ${gestacaoDias} dias; preencha só se não houver cobertura registrada)`}>
              <input
                type="date"
                className={inputCls}
                style={inputStyle}
                value={form.previsaoParto || ""}
                onChange={(e) => setForm({ ...form, previsaoParto: e.target.value })}
              />
            </Field>
          )}

          {form.tipo === "parto" && (
            <Field label="Observações (sexo da cria, ocorrências)">
              <input className={inputCls} style={inputStyle} value={form.detalhe} onChange={(e) => setForm({ ...form, detalhe: e.target.value })} />
            </Field>
          )}

          {salvoAgora && (
            <p className="sm:col-span-2 text-sm rounded-lg px-3 py-2 flex items-center gap-1.5" style={{ backgroundColor: C.pasto50, color: C.pasto900 }} role="status">
              <CheckCircle size={16} /> {salvoAgora}
            </p>
          )}

          <div className="sm:col-span-2 flex flex-wrap justify-end gap-2">
            <button
              onClick={() => {
                setOpen(false);
                setRegistrandoReproducao(null);
                setSalvoAgora("");
                setForm(novoEventoReproducao());
              }}
              className="px-3 py-2 text-sm rounded-lg"
              style={{ color: C.inkSoft }}
            >
              Cancelar
            </button>
            <button
              onClick={() => add(true)}
              disabled={salvando}
              className="px-4 py-2 text-sm rounded-lg font-semibold border"
              style={{ borderColor: C.pasto900, color: C.pasto900, backgroundColor: "#fff" }}
              title="Salva e deixa o formulário aberto para o mesmo animal"
            >
              Salvar e continuar
            </button>
            <button
              onClick={() => add(false)}
              disabled={salvando}
              className="px-4 py-2 text-sm rounded-lg font-semibold"
              style={{ backgroundColor: C.pasto900, color: "#fff" }}
            >
              {salvando ? "Salvando…" : "Salvar evento"}
            </button>
          </div>
        </div>
      )}

      {/* Modal para Registrar Reprodução */}
      {modalReproducao.aberta && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full max-h-[90vh] overflow-y-auto" style={{ boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)" }}>
            {/* Cabeçalho da Modal */}
            <div className="mb-5">
              <h2 className="text-xl font-bold flex items-center gap-2" style={{ color: C.ink }}>
                🧬 Registrar Reprodução
              </h2>
              <p className="text-xs mt-2" style={{ color: C.inkSoft }}>
                Vínculo com cio de origem
              </p>
            </div>

            {/* Informações do Animal e Cio de Origem */}
            <div className="p-3 rounded-lg mb-4" style={{ backgroundColor: C.pasto50 }}>
              <p className="text-sm font-semibold" style={{ color: C.ink }}>
                🐄 {rotuloAnimal(animais.find((a) => a.id === modalReproducao.animalId))}
              </p>
              <p className="text-xs mt-1" style={{ color: C.inkSoft }}>
                Cio de origem: <span style={{ ...fMono, color: C.pasto900 }}>{formatDate(modalReproducao.cioData)}</span>
              </p>
            </div>

            {/* Seleção do Tipo de Reprodução */}
            <Field label="Tipo de reprodução *">
              <div className="flex flex-col gap-2">
                <label className="flex items-center gap-2 cursor-pointer p-2 rounded-lg hover:bg-gray-50">
                  <input
                    type="radio"
                    name="tipoReproducao"
                    value="touro"
                    checked={modalReproducao.tipoReproducao === "touro"}
                    onChange={(e) =>
                      setModalReproducao({
                        ...modalReproducao,
                        tipoReproducao: e.target.value,
                        formReproducao: {
                          ...modalReproducao.formReproducao,
                          dataReproducao: "",
                          touroDados: "",
                          observacoes: "",
                        },
                      })
                    }
                  />
                  <span className="text-sm font-medium">Touro / Monta natural</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer p-2 rounded-lg hover:bg-gray-50">
                  <input
                    type="radio"
                    name="tipoReproducao"
                    value="inseminacao"
                    checked={modalReproducao.tipoReproducao === "inseminacao"}
                    onChange={(e) =>
                      setModalReproducao({
                        ...modalReproducao,
                        tipoReproducao: e.target.value,
                        formReproducao: {
                          ...modalReproducao.formReproducao,
                          dataReproducao: "",
                          touroDados: "",
                          semenCodigo: "",
                        },
                      })
                    }
                  />
                  <span className="text-sm font-medium">Inseminação artificial</span>
                </label>
              </div>
            </Field>

            {/* Campo de Data (obrigatório) */}
            <Field label={modalReproducao.tipoReproducao === "touro" ? "Data da reprodução *" : "Data da inseminação *"}>
              <input
                type="date"
                className={inputCls}
                style={inputStyle}
                value={modalReproducao.formReproducao.dataReproducao}
                min={modalReproducao.cioData}
                max={todayStr()}
                onChange={(e) =>
                  setModalReproducao({
                    ...modalReproducao,
                    formReproducao: { ...modalReproducao.formReproducao, dataReproducao: e.target.value },
                  })
                }
              />
            </Field>

            {/* Campos específicos para Touro */}
            <PrevisaoParto data={preverParto(modalReproducao.formReproducao.dataReproducao)} dias={gestacaoDias} />

            {modalReproducao.tipoReproducao === "touro" && (
              <>
                <Field label="Touro utilizado *">
                  <input
                    type="text"
                    className={inputCls}
                    style={inputStyle}
                    placeholder="Nome ou ID do touro"
                    value={modalReproducao.formReproducao.touroDados}
                    onChange={(e) =>
                      setModalReproducao({
                        ...modalReproducao,
                        formReproducao: { ...modalReproducao.formReproducao, touroDados: e.target.value },
                      })
                    }
                  />
                </Field>
                <Field label="Observação">
                  <input
                    type="text"
                    className={inputCls}
                    style={inputStyle}
                    placeholder="Observações sobre a monta"
                    value={modalReproducao.formReproducao.observacoes}
                    onChange={(e) =>
                      setModalReproducao({
                        ...modalReproducao,
                        formReproducao: { ...modalReproducao.formReproducao, observacoes: e.target.value },
                      })
                    }
                  />
                </Field>
              </>
            )}

            {/* Campos específicos para Inseminação Artificial */}
            {modalReproducao.tipoReproducao === "inseminacao" && (
              <Field label="Código/nome do sêmen ou touro *">
                <input
                  type="text"
                  className={inputCls}
                  style={inputStyle}
                  placeholder="Código do sêmen ou nome do touro"
                  value={modalReproducao.formReproducao.semenCodigo}
                  onChange={(e) =>
                    setModalReproducao({
                      ...modalReproducao,
                      formReproducao: { ...modalReproducao.formReproducao, semenCodigo: e.target.value },
                    })
                  }
                />
              </Field>
            )}

            <div className="mt-3">
              <ErrorText text={erro} />
            </div>

            {/* Botões de Ação */}
            <div className="mt-6 flex gap-2">
              <button
                onClick={() => {
                  setModalReproducao({ ...modalReproducao, aberta: false });
                  setErro("");
                }}
                disabled={salvando}
                className="flex-1 px-3 py-2 rounded-lg text-sm font-semibold"
                style={{ color: C.inkSoft, borderColor: C.line, border: "1px solid" }}
              >
                Cancelar
              </button>
              <button
                onClick={salvarReproducao}
                disabled={salvando}
                className="flex-1 px-3 py-2 rounded-lg text-sm font-semibold text-white transition-opacity"
                style={{ backgroundColor: C.pasto900, opacity: salvando ? 0.6 : 1 }}
              >
                {salvando ? "Salvando…" : "Salvar"}
              </button>
            </div>
          </div>
        </div>
      )}

      {comEventos.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} color={C.inkSoft} className="absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="search"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Buscar por brinco ou nome…"
              className={`${inputCls} w-full pl-9`}
              style={inputStyle}
              aria-label="Buscar vaca no histórico por brinco ou nome"
            />
            {busca && (
              <button onClick={() => setBusca("")} className="absolute right-2 top-1/2 -translate-y-1/2 p-1" aria-label="Limpar busca">
                <X size={14} color={C.inkSoft} />
              </button>
            )}
          </div>
          <button onClick={() => setAbertos(new Set(filtrados.map((g) => g.animal.id)))} className="px-3 py-2 rounded-lg text-sm border" style={{ borderColor: C.line, color: C.pasto900, backgroundColor: "#fff" }}>
            Expandir todas
          </button>
          <button onClick={() => setAbertos(new Set())} className="px-3 py-2 rounded-lg text-sm border" style={{ borderColor: C.line, color: C.pasto900, backgroundColor: "#fff" }}>
            Recolher todas
          </button>
        </div>
      )}

      {comEventos.length === 0 ? (
        <EmptyState text="Nenhum evento reprodutivo registrado. Cios, inseminações e partos aparecem aqui." />
      ) : filtrados.length === 0 ? (
        <EmptyState text={`Nenhuma vaca com brinco ou nome "${busca.trim()}" no histórico.`} />
      ) : (
        <div className="flex flex-col gap-3">
          {filtrados.map(({ animal, eventos }) => {
            const aberto = estaAberto(animal.id);
            const ultimo = eventos[0];
            const pendente = coberturaAguardandoDiagnostico(eventos);
            return (
            <div key={animal.id} className="rounded-xl border bg-white" style={{ borderColor: C.line }}>
              <button
                onClick={() => alternar(animal.id)}
                className="w-full flex items-center gap-2 p-4 text-left"
                aria-expanded={aberto}
              >
                {aberto ? <ChevronDown size={18} color={C.inkSoft} /> : <ChevronRight size={18} color={C.inkSoft} />}
                <div className="min-w-0 flex-1">
                  <h3 className="font-semibold" style={{ color: C.ink }}>{rotuloAnimal(animal)}</h3>
                  <p className="text-xs" style={{ color: C.inkSoft }}>
                    {eventos.length} evento(s) · último: {nomeEvento(ultimo)} em {formatDate(ultimo.data)}
                  </p>
                </div>
                {pendente && (
                  <span className="text-[11px] px-2 py-0.5 rounded-full font-semibold shrink-0" style={{ backgroundColor: C.brinco + "22", color: C.brincoDark }}>
                    Aguardando diagnóstico
                  </span>
                )}
              </button>

              {pendente && (
                <DiagnosticoRapido animalId={animal.id} cobertura={pendente} onRegistrar={registrarDiagnostico} />
              )}

              {aberto && (
              <div className="space-y-3 px-4 pb-4">
                {eventos.map((e) => (
                  <div key={e.id} className="p-3 rounded-lg border" style={{ borderColor: C.line, backgroundColor: "#fafaf8" }}>
                    <div className="flex items-start gap-3">
                      {/* Ícone de status */}
                      <div className="pt-1">
                        {e.tipo === "cio_detectado" && !e.dataConfirmacao ? (
                          <Circle size={20} color={C.cio} fill={C.cio} />
                        ) : e.tipo === "cio_confirmado" || (e.tipo === "cio_detectado" && e.dataConfirmacao) ? (
                          <CheckCircle size={20} color={C.pasto900} fill={C.pasto900} />
                        ) : (
                          <div className="w-5 h-5 rounded-full" style={{ backgroundColor: C.pasto50 }} />
                        )}
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs px-2 py-1 rounded-md" style={{ ...fMono, backgroundColor: C.pasto50, color: C.pasto900 }}>
                            {formatDate(e.data)}
                          </span>
                          <p className="text-sm font-medium" style={{ color: C.ink }}>
                            {e.dataConfirmacao ? "🟢 Cio confirmado" : e.tipo === "cio_detectado" ? "🔴 Cio detectado" : TIPOS_REPRO.find((t) => t.v === e.tipo)?.l}
                          </p>
                        </div>

                        {e.dataConfirmacao && (
                          <p className="text-xs mt-1" style={{ color: C.inkSoft }}>
                            Confirmado em {formatDate(e.dataConfirmacao)}
                          </p>
                        )}

                        {(e.detalhe || e.resultado || e.previsaoParto) && (
                          <p className="text-xs mt-1" style={{ color: C.inkSoft }}>
                            {[
                              e.resultado && (e.resultado === "prenhe" ? "✓ Prenhe" : "✗ Vazia"),
                              e.detalhe,
                              e.previsaoParto && `Parto previsto: ${formatDate(e.previsaoParto)}`,
                            ].filter(Boolean).join(" · ")}
                          </p>
                        )}
                      </div>

                      <button onClick={() => remove(e)} className="p-1.5 rounded-md hover:bg-gray-100 shrink-0" title="Remover evento">
                        <Trash2 size={16} color={C.cio} />
                      </button>
                    </div>

                    {/* Ações rápidas */}
                    {e.tipo === "cio_detectado" && !e.dataConfirmacao && (
                      <div className="mt-3 flex gap-2">
                        <button
                          onClick={() => confirmarCio(e.id)}
                          disabled={salvando}
                          className="flex-1 px-3 py-2 rounded-lg text-sm font-semibold text-white transition-opacity"
                          style={{ backgroundColor: C.pasto900, opacity: salvando ? 0.6 : 1 }}
                        >
                          {salvando ? "Confirmando…" : "✓ Confirmar Cio"}
                        </button>
                      </div>
                    )}

                    {((e.tipo === "cio_detectado" && e.dataConfirmacao) || e.tipo === "cio_confirmado") && cioJaCoberto(e.id) ? (
                      <p className="text-xs mt-2" style={{ color: C.pasto900 }}>✓ Cobertura registrada para este cio</p>
                    ) : (e.tipo === "cio_detectado" && e.dataConfirmacao) || e.tipo === "cio_confirmado" ? (
                      <div className="mt-3 flex gap-2">
                        <button
                          onClick={() => abrirRegistroReproducao(e.id, animal.id)}
                          className="flex-1 px-3 py-2 rounded-lg text-sm font-semibold text-white"
                          style={{ backgroundColor: C.cio }}
                        >
                          + Registrar reprodução
                        </button>
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>
              )}
            </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const nomeEvento = (e) =>
  e.dataConfirmacao ? "Cio confirmado" : TIPOS_REPRO.find((t) => t.v === e.tipo)?.l || e.tipo;

/**
 * Cobertura mais recente que ainda espera o diagnóstico de prenhez (eventos do mais recente ao mais antigo).
 * Diagnóstico, prenhez confirmada, perda, parto, novo cio, secagem ou pré-parto depois dela encerram a espera.
 */
function coberturaAguardandoDiagnostico(eventos) {
  for (const e of eventos) {
    if (["inseminacao", "reproducao"].includes(e.tipo)) return e;
    if (["diagnostico_prenhez", "prenhez_confirmada", "perda_gestacional", "parto",
      "cio_detectado", "cio_confirmado", "secagem", "pre_parto"].includes(e.tipo)) return null;
  }
  return null;
}

/** Registra o diagnóstico de prenhez direto no histórico: escolhe o resultado e confirma com a data. */
function DiagnosticoRapido({ animalId, cobertura, onRegistrar }) {
  const [resultado, setResultado] = useState(null);
  const [data, setData] = useState(todayStr());
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const dias = Math.round((new Date(todayStr()) - new Date(cobertura.data)) / 86400000);

  const confirmar = async () => {
    setSalvando(true);
    setErro("");
    try {
      await onRegistrar(animalId, resultado, data); // o aviso some porque a cobertura deixa de estar pendente
    } catch (e) {
      setErro(e.message || "Não foi possível registrar o diagnóstico.");
      setSalvando(false);
    }
  };

  const botao = "px-3 py-1.5 rounded-lg text-sm font-semibold";
  return (
    <div className="mx-4 mb-3 rounded-lg px-3 py-2 flex flex-wrap items-center gap-2" style={{ backgroundColor: C.brinco + "14" }}>
      <Stethoscope size={16} color={C.brincoDark} />
      <span className="text-sm" style={{ color: C.ink }}>
        {nomeEvento(cobertura)} em {formatDate(cobertura.data)} ({dias} dias)
        {!resultado && " — resultado do diagnóstico:"}
      </span>
      {!resultado ? (
        <>
          <button onClick={() => setResultado("prenhe")} className={botao} style={{ backgroundColor: C.pasto900, color: "#fff" }}>Prenhe</button>
          <button onClick={() => setResultado("vazia")} className={`${botao} border`} style={{ borderColor: C.pasto900, color: C.pasto900, backgroundColor: "#fff" }}>Vazia</button>
        </>
      ) : (
        <>
          <span className="text-sm" style={{ color: C.ink }}>registrar como <strong>{resultado === "prenhe" ? "PRENHE" : "VAZIA"}</strong> em</span>
          <input type="date" value={data} min={cobertura.data} max={todayStr()} onChange={(e) => e.target.value && setData(e.target.value)}
            className={inputCls} style={inputStyle} aria-label="Data do diagnóstico" />
          <button onClick={confirmar} disabled={salvando} className={`${botao} flex items-center gap-1`} style={{ backgroundColor: C.brinco, color: "#fff" }}>
            {salvando ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />} Confirmar
          </button>
          <button onClick={() => { setResultado(null); setErro(""); }} disabled={salvando} className="px-2 py-1.5 text-sm" style={{ color: C.inkSoft }}>Cancelar</button>
        </>
      )}
      {erro && <p className="w-full text-xs" style={{ color: C.cio }} role="alert">{erro}</p>}
    </div>
  );
}

function PrevisaoParto({ data, dias }) {
  if (!data) return null;

  return (
    <div className="rounded-lg px-3 py-2 text-sm" style={{ backgroundColor: C.pasto50, color: C.pasto900 }}>
      Parto previsto: <strong>{formatDate(data)}</strong> <span className="text-xs">({dias} dias de gestacao)</span>
    </div>
  );
}
