import { useState } from "react";
import { WifiOff, CloudUpload, AlertTriangle, Loader2, ChevronDown, ChevronUp, RotateCcw, Trash2 } from "lucide-react";
import { useOffline } from "../hooks/useOffline";
import { filaLocal } from "../services/offline";
import { authStorage, sincronizarPendentes } from "../services/api";
import { C } from "../utils/tokens";

const plural = (n, um, varios) => `${n} ${n === 1 ? um : varios}`;

function quando(ms) {
  const d = new Date(ms);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)} às ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Faixa abaixo do cabeçalho com o estado do modo offline: sem internet, lançamentos
 * aguardando envio, lançamentos recusados pela API e sessão expirada. Some quando está tudo em dia.
 */
export default function OfflineBanner() {
  const { online, pendentes, falhas, sincronizando, sessaoExpirada, dadosSalvosEm } = useOffline();
  const [aberto, setAberto] = useState(false);
  const [lista, setLista] = useState([]);

  const temAlgo = !online || pendentes > 0 || falhas.length > 0 || sessaoExpirada || dadosSalvosEm !== null;
  if (!temAlgo) return null;

  const fazendaId = authStorage.getFazendaId();
  const alternarLista = async () => {
    if (!aberto) setLista((await filaLocal.listar(fazendaId)).filter((i) => !i.erro));
    setAberto((a) => !a);
  };
  const tentarDeNovo = async (item) => {
    await filaLocal.limparErro(item.localId, fazendaId);
    sincronizarPendentes();
  };
  const descartar = async (item) => {
    if (!window.confirm(`Descartar este lançamento? Ele não será enviado.\n\n${item.descricao}`)) return;
    await filaLocal.remover(item.localId, fazendaId);
  };

  return (
    <div className="px-4 sm:px-6 py-3 text-sm" style={{ backgroundColor: online ? "#FBF3E6" : "#EEF1EA", borderBottom: `1px solid ${C.line}` }}>
      <div className="max-w-5xl mx-auto flex flex-col gap-2">
        {!online && (
          <p className="flex items-start gap-2" style={{ color: C.ink }}>
            <WifiOff size={16} className="mt-0.5 shrink-0" color={C.inkSoft} />
            <span>
              <strong>Sem internet.</strong> Você pode continuar lançando ordenha, reprodução, saúde e financeiro: fica tudo guardado
              neste aparelho e é enviado sozinho quando o sinal voltar.
              {dadosSalvosEm !== null && <> Os dados na tela são de {quando(dadosSalvosEm)}.</>}
            </span>
          </p>
        )}

        {online && dadosSalvosEm !== null && pendentes === 0 && (
          <p className="flex items-center gap-2" style={{ color: C.inkSoft }}>
            <Loader2 size={14} className="animate-spin shrink-0" />
            Mostrando os dados guardados em {quando(dadosSalvosEm)}. Atualizando assim que a API responder…
          </p>
        )}

        {sessaoExpirada && (
          <p className="flex items-start gap-2" style={{ color: C.cio }}>
            <AlertTriangle size={16} className="mt-0.5 shrink-0" />
            <span>
              Sua sessão expirou. Saia e entre de novo para enviar os lançamentos pendentes — eles continuam guardados neste aparelho.
            </span>
          </p>
        )}

        {pendentes > 0 && (
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <CloudUpload size={16} className="shrink-0" color={C.brincoDark} />
              <span style={{ color: C.ink }}>
                <strong>{plural(pendentes, "lançamento aguardando", "lançamentos aguardando")}</strong> envio.
              </span>
              <button onClick={alternarLista} className="flex items-center gap-1 text-xs underline" style={{ color: C.inkSoft }}>
                {aberto ? "ocultar" : "ver quais"} {aberto ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
              </button>
              <button
                onClick={() => sincronizarPendentes()}
                disabled={sincronizando}
                className="ml-auto flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold disabled:opacity-60"
                style={{ backgroundColor: C.pasto900, color: "#fff" }}
              >
                {sincronizando ? <Loader2 size={13} className="animate-spin" /> : <CloudUpload size={13} />}
                {sincronizando ? "Enviando…" : "Enviar agora"}
              </button>
            </div>
            {aberto && (
              <ul className="mt-2 ml-6 list-disc text-xs flex flex-col gap-0.5" style={{ color: C.inkSoft }}>
                {lista.map((i) => (
                  <li key={i.localId}>{i.descricao}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        {falhas.length > 0 && (
          <div className="rounded-lg p-2.5" style={{ backgroundColor: "#fff", border: `1px solid ${C.cio}55` }}>
            <p className="flex items-center gap-2 font-semibold mb-1.5" style={{ color: C.cio }}>
              <AlertTriangle size={15} />
              {plural(falhas.length, "lançamento foi recusado", "lançamentos foram recusados")} pela API
            </p>
            <ul className="flex flex-col gap-1.5">
              {falhas.map((item) => (
                <li key={item.localId} className="flex items-start gap-2 text-xs">
                  <div className="flex-1 min-w-0">
                    <p style={{ color: C.ink }}>{item.descricao}</p>
                    <p style={{ color: C.cio }}>{item.erro}</p>
                  </div>
                  <button onClick={() => tentarDeNovo(item)} className="p-1 rounded hover:bg-gray-100" title="Tentar enviar de novo">
                    <RotateCcw size={14} color={C.inkSoft} />
                  </button>
                  <button onClick={() => descartar(item)} className="p-1 rounded hover:bg-gray-100" title="Descartar">
                    <Trash2 size={14} color={C.cio} />
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
