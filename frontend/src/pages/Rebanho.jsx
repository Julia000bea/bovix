import { useEffect, useRef, useState } from "react";
import { Plus, Search, Trash2 } from "lucide-react";
import { Field, Pill, EmptyState, ErrorText, inputCls, inputStyle } from "../components/ui";
import PhotoUpload from "../components/PhotoUpload";
import { C, CATEGORIAS, STATUS_ANIMAL, TIPOS_REPRO, TIPOS_SAUDE, fMono } from "../utils/tokens";
import { formatDate } from "../utils/date";
import { novoAnimal } from "../models/Animal";
import { animaisService } from "../services/animaisService";

export default function Rebanho({ data }) {
  const { animais, setAnimais, reproducao, setReproducao, saude, setSaude, setOrdenha } = data;
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [form, setForm] = useState(novoAnimal());
  const [fotoPendente, setFotoPendente] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [animalSelecionado, setAnimalSelecionado] = useState(null);
  const [formEdicao, setFormEdicao] = useState({});
  const [erro, setErro] = useState("");
  const camposCadastroRef = useRef([]);

  const focarBrinco = () => {
    requestAnimationFrame(() => camposCadastroRef.current[0]?.focus());
  };

  useEffect(() => {
    if (open) focarBrinco();
  }, [open]);

  const avancarCampo = (event, proximoCampo) => {
    if (event.key !== "Enter" || event.nativeEvent.isComposing) return;
    event.preventDefault();

    if (proximoCampo === null) {
      addAnimal();
      return;
    }
    camposCadastroRef.current[proximoCampo]?.focus();
  };

  const addAnimal = async () => {
    if (!form.brinco || salvando) return;
    setSalvando(true);
    setErro("");
    try {
      let criado = await animaisService.criar(form);
      // o animal já existe a partir daqui: falha na foto não pode "sumir" com ele da lista
      if (fotoPendente) {
        try {
          criado = await animaisService.enviarFoto(criado.id, fotoPendente);
        } catch (e) {
          setErro(`Animal #${criado.brinco} salvo, mas a foto não foi enviada: ${e.message}`);
        }
      }
      setAnimais((prev) => [...prev, criado]);
      setForm(novoAnimal());
      setFotoPendente(null);
      focarBrinco();
    } catch (e) {
      setErro(e.message || "Não foi possível salvar o animal.");
    } finally {
      setSalvando(false);
    }
  };

  const updateStatus = async (id, status) => {
    const atual = animais.find((a) => a.id === id);
    setErro("");
    try {
      const atualizado = await animaisService.atualizar(id, { ...atual, status });
      setAnimais((prev) => prev.map((a) => (a.id === id ? { ...a, status: atualizado.status } : a)));
    } catch (e) {
      setErro(e.message || "Não foi possível alterar o status.");
    }
  };

  const removeAnimal = async (animal) => {
    const confirmou = window.confirm(
      `Excluir #${animal.brinco} apaga também TODO o histórico dele (reprodução, saúde e ordenha) e não pode ser desfeito.\n\n` +
      `Se o animal foi vendido ou morreu, prefira mudar o status para "Vendida" ou "Morta".\n\nExcluir mesmo assim?`
    );
    if (!confirmou) return;
    setErro("");
    try {
      await animaisService.remover(animal.id);
      setAnimais((prev) => prev.filter((a) => a.id !== animal.id));
      // o banco remove o histórico em cascata; mantém a tela coerente sem recarregar
      setReproducao((prev) => prev.filter((e) => e.animalId !== animal.id));
      setSaude((prev) => prev.filter((e) => e.animalId !== animal.id));
      setOrdenha((prev) => prev.filter((e) => e.animalId !== animal.id));
    } catch (e) {
      setErro(e.message || "Não foi possível excluir o animal.");
    }
  };

  const abrirEdicao = (animal) => {
    setAnimalSelecionado(animal);
    setFormEdicao(animal);
  };

  const salvarEdicao = async () => {
    setSalvando(true);
    setErro("");
    try {
      const atualizado = await animaisService.atualizar(animalSelecionado.id, formEdicao);
      setAnimais((prev) => prev.map((a) => (a.id === animalSelecionado.id ? atualizado : a)));
      setAnimalSelecionado(null);
    } catch (e) {
      setErro(e.message || "Não foi possível salvar as alterações.");
    } finally {
      setSalvando(false);
    }
  };

  const cancelarEdicao = () => {
    setAnimalSelecionado(null);
    setFormEdicao({});
    setErro("");
  };

  const filtered = animais.filter(
    (a) => a.brinco.toLowerCase().includes(q.toLowerCase()) || (a.nome || "").toLowerCase().includes(q.toLowerCase())
  );

  const historicoAnimal = animalSelecionado
    ? [
        ...reproducao
          .filter((evento) => Number(evento.animalId) === animalSelecionado.id)
          .map((evento) => ({
            ...evento,
            origem: "reproducao",
            titulo: TIPOS_REPRO.find((tipo) => tipo.v === evento.tipo)?.l || evento.tipo,
            detalhe: [
              evento.resultado === "prenhe" ? "Prenhe" : evento.resultado === "vazia" ? "Vazia" : evento.resultado,
              evento.detalhe,
              evento.previsaoParto && `Parto previsto: ${formatDate(evento.previsaoParto)}`,
            ].filter(Boolean).join(" · "),
          })),
        ...saude
          .filter((evento) => Number(evento.animalId) === animalSelecionado.id)
          .map((evento) => ({
            ...evento,
            origem: "saude",
            titulo: `${TIPOS_SAUDE.find((tipo) => tipo.v === evento.tipo)?.l || "Saúde"}: ${evento.produto}`,
            detalhe: [
              evento.carenciaLeiteDias && `Carência: ${evento.carenciaLeiteDias} dias`,
              evento.observacoes,
            ].filter(Boolean).join(" · "),
          })),
      ].sort((a, b) => new Date(b.data) - new Date(a.data))
    : [];

  return (
    <div className="flex flex-col gap-4">
      {!animalSelecionado && <ErrorText text={erro} />}
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
            <Field label="Brinco (nº) *"><input ref={(el) => { camposCadastroRef.current[0] = el; }} className={inputCls} style={inputStyle} value={form.brinco} onChange={(e) => setForm({ ...form, brinco: e.target.value })} onKeyDown={(e) => avancarCampo(e, 1)} /></Field>
            <Field label="Nome"><input ref={(el) => { camposCadastroRef.current[1] = el; }} className={inputCls} style={inputStyle} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} onKeyDown={(e) => avancarCampo(e, 2)} /></Field>
            <Field label="Raça">
              <select ref={(el) => { camposCadastroRef.current[2] = el; }} className={inputCls} style={inputStyle} value={form.raca} onChange={(e) => setForm({ ...form, raca: e.target.value })} onKeyDown={(e) => avancarCampo(e, 3)}>
                <option value="">Não informado</option>
                <option value="Holandês">Holandês</option>
                <option value="Jersey">Jersey</option>
              </select>
            </Field>
            <Field label="Nascimento"><input ref={(el) => { camposCadastroRef.current[3] = el; }} type="date" className={inputCls} style={inputStyle} value={form.nascimento} onChange={(e) => setForm({ ...form, nascimento: e.target.value })} onKeyDown={(e) => avancarCampo(e, 4)} /></Field>
            <Field label="Categoria">
              <select ref={(el) => { camposCadastroRef.current[4] = el; }} className={inputCls} style={inputStyle} value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })} onKeyDown={(e) => avancarCampo(e, 5)}>
                {CATEGORIAS.map((c) => <option key={c}>{c}</option>)}
              </select>
            </Field>
            <Field label="Notas"><input ref={(el) => { camposCadastroRef.current[5] = el; }} className={inputCls} style={inputStyle} value={form.notas} onChange={(e) => setForm({ ...form, notas: e.target.value })} onKeyDown={(e) => avancarCampo(e, null)} /></Field>
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
            <div
              key={a.id}
              onClick={() => abrirEdicao(a)}
              className="rounded-xl border bg-white p-3 flex items-center gap-3 cursor-pointer hover:shadow-md transition-shadow"
              style={{ borderColor: C.line, opacity: a.status === "Ativa" ? 1 : 0.55 }}
            >
              {/* clicar na foto troca a foto — não abre a edição do animal */}
              <div onClick={(e) => e.stopPropagation()}>
                <PhotoUpload
                  animalId={a.id}
                  fotoUrl={a.fotoUrl}
                  size={48}
                  onUploaded={(atualizado) => setAnimais((prev) => prev.map((x) => (x.id === a.id ? { ...x, fotoUrl: atualizado.fotoUrl } : x)))}
                />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span style={fMono} className="text-lg font-semibold" >#{a.brinco}</span>
                  <span className="font-semibold text-xs" style={{ color: C.ink }}>{a.nome || "Sem nome"}</span>
                  <Pill color={C.leite}>{a.categoria}</Pill>
                  {a.status !== "Ativa" && <Pill color={C.terra}>{a.status}</Pill>}
                </div>
                <p className="text-xs mt-0.5" style={{ color: C.inkSoft }}>
                  {a.raca || "raça não informada"} {a.nascimento && `· nasc. ${formatDate(a.nascimento)}`}
                </p>
              </div>
              <select onClick={(e) => e.stopPropagation()} value={a.status} onChange={(e) => updateStatus(a.id, e.target.value)} className="text-xs rounded-md border px-2 py-1.5" style={{ borderColor: C.line, color: C.inkSoft }}>
                {STATUS_ANIMAL.map((s) => <option key={s}>{s}</option>)}
              </select>
              <button onClick={(e) => { e.stopPropagation(); removeAnimal(a); }} className="p-1.5 rounded-md hover:bg-gray-100 shrink-0" title="Remover animal">
                <Trash2 size={16} color={C.cio} />
              </button>
            </div>
          ))}
        </div>
      )}

      {animalSelecionado && (
        <div className="fixed inset-0 bg-black/50 flex items-end sm:items-center justify-center z-50">
          <div className="rounded-t-2xl sm:rounded-2xl bg-white p-4 w-full sm:max-w-md max-h-[90vh] overflow-y-auto sm:shadow-lg" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold" style={{ color: C.ink }}>Editar Animal</h2>
              <button onClick={cancelarEdicao} className="text-xl" style={{ color: C.inkSoft }}>✕</button>
            </div>

            <div className="flex flex-col gap-3">
              <PhotoUpload
                animalId={formEdicao.id}
                fotoUrl={formEdicao.fotoUrl}
                size={72}
                onUploaded={(atualizado) => setFormEdicao({ ...formEdicao, fotoUrl: atualizado.fotoUrl })}
              />

              <section className="rounded-xl border p-3" style={{ borderColor: C.line, backgroundColor: C.pasto50 }}>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <h3 className="text-sm font-semibold" style={{ color: C.ink }}>Histórico do animal</h3>
                  <span className="text-xs" style={{ ...fMono, color: C.inkSoft }}>#{animalSelecionado.brinco}</span>
                </div>
                {historicoAnimal.length === 0 ? (
                  <p className="text-xs" style={{ color: C.inkSoft }}>Nenhum evento de reprodução ou saúde registrado.</p>
                ) : (
                  <div className="flex flex-col gap-2 max-h-52 overflow-y-auto pr-1">
                    {historicoAnimal.map((evento) => (
                      <div key={`${evento.origem}-${evento.id}`} className="rounded-lg bg-white px-2.5 py-2 border" style={{ borderColor: C.line }}>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] rounded px-1.5 py-0.5 shrink-0" style={{ ...fMono, backgroundColor: evento.origem === "reproducao" ? C.cio + "22" : C.leite + "22", color: evento.origem === "reproducao" ? C.cio : C.leite }}>
                            {evento.origem === "reproducao" ? "REPRODUÇÃO" : "SAÚDE"}
                          </span>
                          <span className="text-xs" style={{ ...fMono, color: C.inkSoft }}>{formatDate(evento.data)}</span>
                        </div>
                        <p className="text-xs font-medium mt-1" style={{ color: C.ink }}>{evento.titulo}</p>
                        {evento.detalhe && <p className="text-xs mt-0.5" style={{ color: C.inkSoft }}>{evento.detalhe}</p>}
                      </div>
                    ))}
                  </div>
                )}
              </section>

              <Field label="Brinco (nº) *">
                <input className={inputCls} style={inputStyle} value={formEdicao.brinco || ""} onChange={(e) => setFormEdicao({ ...formEdicao, brinco: e.target.value })} />
              </Field>
              <Field label="Nome">
                <input className={inputCls} style={inputStyle} value={formEdicao.nome || ""} onChange={(e) => setFormEdicao({ ...formEdicao, nome: e.target.value })} />
              </Field>
              <Field label="Raça">
                <input className={inputCls} style={inputStyle} value={formEdicao.raca || ""} onChange={(e) => setFormEdicao({ ...formEdicao, raca: e.target.value })} />
              </Field>
              <Field label="Nascimento">
                <input type="date" className={inputCls} style={inputStyle} value={formEdicao.nascimento || ""} onChange={(e) => setFormEdicao({ ...formEdicao, nascimento: e.target.value })} />
              </Field>
              <Field label="Categoria">
                <select className={inputCls} style={inputStyle} value={formEdicao.categoria || ""} onChange={(e) => setFormEdicao({ ...formEdicao, categoria: e.target.value })}>
                  {CATEGORIAS.map((c) => <option key={c}>{c}</option>)}
                </select>
              </Field>
              <Field label="Notas">
                <input className={inputCls} style={inputStyle} value={formEdicao.notas || ""} onChange={(e) => setFormEdicao({ ...formEdicao, notas: e.target.value })} />
              </Field>

              <ErrorText text={erro} />
              <div className="flex justify-end gap-2 mt-4">
                <button onClick={cancelarEdicao} className="px-4 py-2 text-sm rounded-lg" style={{ color: C.inkSoft }}>Cancelar</button>
                <button onClick={salvarEdicao} disabled={salvando} className="px-4 py-2 text-sm rounded-lg font-semibold" style={{ backgroundColor: C.pasto900, color: "#fff" }}>
                  {salvando ? "Salvando…" : "Salvar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
