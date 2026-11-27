// Cliente HTTP central: todas as chamadas à API Java passam por aqui.
//
// Modo offline:
//  - consultas (GET) guardam a última resposta no aparelho; sem internet, devolvem essa cópia;
//  - lançamentos marcados como "offline" (ordenha, reprodução, saúde, financeiro) entram numa
//    fila quando não há internet e são enviados sozinhos quando a conexão volta;
//  - o resto (excluir, editar, configurações, cadastro de animal...) continua precisando de internet.
import {
  atualizarContagem,
  atualizarEstadoOffline,
  cacheLocal,
  filaLocal,
  lerEstadoOffline,
  marcarDadosSalvos,
  pedirRecargaDosDados,
} from "./offline";

const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();
if (import.meta.env.PROD && !configuredApiUrl) {
  throw new Error("VITE_API_URL não foi configurada no build de produção.");
}
const API_URL = (configuredApiUrl || "http://localhost:8080/api").replace(/\/+$/, "");

const TOKEN_KEY = "bovix_token";
const FAZENDA_KEY = "bovix_fazenda_id";

// Com uma cópia guardada no aparelho, não vale a pena esperar mais que isso por um sinal fraco.
// (A primeira chamada do dia ao Render grátis pode demorar ~1 min; nesse caso a tela mostra a
// cópia e se atualiza sozinha quando a API responder — ver sincronizarPendentes.)
const ESPERA_COM_COPIA_MS = 20000;

export const authStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(FAZENDA_KEY);
    // dados da fazenda guardados no aparelho saem junto com a conta (aparelho compartilhado).
    // A fila de lançamentos NÃO é apagada: ela é enviada quando a mesma conta entrar de novo.
    cacheLocal.limpar();
    atualizarEstadoOffline({ dadosSalvosEm: null, pendentes: 0, falhas: [] });
  },
  getFazendaId: () => Number(localStorage.getItem(FAZENDA_KEY)) || null,
  setFazendaId: (id) => localStorage.setItem(FAZENDA_KEY, String(id)),
};

export const FAZENDA_ID = () => authStorage.getFazendaId();

/** Falha de rede: sem internet, sinal fraco demais ou API fora do ar. */
export class ErroDeConexao extends Error {
  constructor(mensagem, opcoes) {
    super(mensagem, opcoes);
    this.name = "ErroDeConexao";
  }
}

function marcarOnline() {
  const estavaOffline = !lerEstadoOffline().online;
  atualizarEstadoOffline({ online: true });
  // a conexão voltou: envia o que ficou na fila e atualiza o que estava na tela
  if (estavaOffline) sincronizarPendentes();
}

function marcarOffline() {
  atualizarEstadoOffline({ online: false });
}

function cabecalhos(options = {}) {
  const token = authStorage.getToken();
  return {
    ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
    ...options.headers,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function buscar(path, options = {}, esperaMs = 0) {
  if (typeof navigator !== "undefined" && navigator.onLine === false) {
    marcarOffline();
    throw new ErroDeConexao("Sem conexão com a internet.");
  }
  const controle = esperaMs ? new AbortController() : null;
  const timer = controle ? setTimeout(() => controle.abort(), esperaMs) : null;
  try {
    const res = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: cabecalhos(options),
      ...(controle ? { signal: controle.signal } : {}),
    });
    marcarOnline();
    return res;
  } catch (error) {
    marcarOffline();
    throw new ErroDeConexao(
      `Sem conexão com a API do Bovix (${API_URL}). Verifique a internet e tente de novo.`,
      { cause: error },
    );
  } finally {
    if (timer) clearTimeout(timer);
  }
}

async function request(path, options = {}, esperaMs = 0) {
  const res = await buscar(path, options, esperaMs);

  // Rotas /auth/* (login, registro, me) tratam o 401 como erro normal: recarregar aqui
  // engoliria a mensagem "E-mail ou senha inválidos".
  if (res.status === 401 && !path.startsWith("/auth/")) {
    authStorage.clear();
    window.location.reload(); // sessão expirada/token inválido: volta para o login
    return null;
  }

  if (!res.ok) {
    const corpo = await res.json().catch(() => ({}));
    throw new Error(corpo.erro || `Erro ${res.status} em ${path}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

// ---------------------------------------------------------------- consultas com cópia local

function podeGuardar(path) {
  return !path.startsWith("/auth/") || path === "/auth/me";
}

/**
 * GET com cópia no aparelho. `chaveCache` permite usar a mesma cópia para consultas cujo
 * endereço muda todo dia (ex.: "últimos 60 dias", que leva a data de hoje na URL).
 */
async function get(path, { chaveCache } = {}) {
  if (!podeGuardar(path)) return request(path);
  const chave = chaveCache || path;
  const copia = await cacheLocal.ler(chave);
  try {
    const dados = await request(path, {}, copia ? ESPERA_COM_COPIA_MS : 0);
    if (dados !== null && dados !== undefined) cacheLocal.salvar(chave, dados);
    return dados;
  } catch (e) {
    if (e instanceof ErroDeConexao && copia) {
      marcarDadosSalvos(copia.salvoEm);
      return copia.dados;
    }
    if (e instanceof ErroDeConexao) {
      throw new ErroDeConexao(
        "Sem internet, e este aparelho ainda não tem os dados da fazenda guardados. Abra o Bovix uma vez com internet para poder usá-lo offline.",
        { cause: e },
      );
    }
    throw e;
  }
}

// ---------------------------------------------------------------- lançamentos com fila offline

/**
 * POST/PUT que, sem internet, entra na fila em vez de falhar.
 * `offline` = { colecao: "ordenha", descricao: "Ordenha 25/11 · 320 L",
 *               provisorio: (localId) => objeto(s) que a tela mostra até o envio }
 */
async function escrever(method, path, body, offline) {
  try {
    return await request(path, { method, body: JSON.stringify(body) });
  } catch (e) {
    if (!(e instanceof ErroDeConexao)) throw e;
    if (!offline) {
      throw new ErroDeConexao("Sem internet: esta ação precisa de conexão. Tente de novo quando o sinal voltar.", { cause: e });
    }
    const item = await filaLocal.adicionar({
      method,
      path,
      body,
      descricao: offline.descricao,
      fazendaId: authStorage.getFazendaId(),
      colecao: offline.colecao,
      montarRegistros: offline.provisorio,
    });
    return item.resposta;
  }
}

const ID_LOCAL = /\/(local-[a-z0-9-]+)(?::\d+)?$/;

async function del(path) {
  // registro que ainda está só no aparelho: basta tirá-lo da fila
  const local = path.match(ID_LOCAL);
  if (local) {
    await filaLocal.remover(local[1], authStorage.getFazendaId());
    return null;
  }
  try {
    return await request(path, { method: "DELETE" });
  } catch (e) {
    if (e instanceof ErroDeConexao) {
      throw new ErroDeConexao("Sem internet: excluir precisa de conexão. Tente de novo quando o sinal voltar.", { cause: e });
    }
    throw e;
  }
}

// ---------------------------------------------------------------- sincronização

let sincronizacaoEmAndamento = null;

/**
 * Envia a fila para a API, do mais antigo para o mais novo. Para no primeiro problema de
 * conexão (tenta de novo depois). Se a API recusar um lançamento (ex.: data no futuro), ele
 * fica marcado com o erro para a pessoa corrigir ou descartar — os outros seguem.
 */
export function sincronizarPendentes() {
  if (!sincronizacaoEmAndamento) {
    sincronizacaoEmAndamento = executarSincronizacao().finally(() => {
      sincronizacaoEmAndamento = null;
    });
  }
  return sincronizacaoEmAndamento;
}

async function executarSincronizacao() {
  const fazendaId = authStorage.getFazendaId();
  if (!authStorage.getToken() || !fazendaId) return;

  const fila = (await filaLocal.listar(fazendaId)).filter((i) => !i.erro);
  const precisaAtualizarTela = lerEstadoOffline().dadosSalvosEm !== null;

  if (fila.length === 0) {
    // nada a enviar: só confere se a API voltou, para trocar a cópia guardada pelos dados atuais
    if (precisaAtualizarTela) {
      try {
        await buscar("/health", {}, ESPERA_COM_COPIA_MS);
        pedirRecargaDosDados();
      } catch {
        /* continua offline */
      }
    }
    return;
  }

  atualizarEstadoOffline({ sincronizando: true });
  let enviados = 0;
  try {
    for (const item of fila) {
      let res;
      try {
        res = await buscar(item.path, { method: item.method, body: JSON.stringify(item.body) });
      } catch {
        break; // sem conexão: tenta de novo mais tarde
      }
      if (res.status === 401) {
        atualizarEstadoOffline({ sessaoExpirada: true });
        break;
      }
      if (res.ok) {
        await filaLocal.remover(item.localId, fazendaId);
        enviados++;
        continue;
      }
      if (res.status >= 500) break; // problema na API: tenta de novo mais tarde
      const corpo = await res.json().catch(() => ({}));
      await filaLocal.marcarErro(item, corpo.erro || `A API recusou este lançamento (erro ${res.status}).`);
    }
  } finally {
    await atualizarContagem(fazendaId);
    atualizarEstadoOffline({ sincronizando: false, ...(enviados > 0 ? { sessaoExpirada: false } : {}) });
  }
  if (enviados > 0 || precisaAtualizarTela) pedirRecargaDosDados();
}

/** Liga o envio automático: quando a internet volta, ao reabrir o app e a cada 30 s. */
export function iniciarSincronizacaoAutomatica() {
  const tentar = () => {
    const e = lerEstadoOffline();
    if (e.pendentes > 0 || !e.online || e.dadosSalvosEm !== null) sincronizarPendentes();
  };
  const aoVoltarInternet = () => {
    atualizarEstadoOffline({ online: true });
    sincronizarPendentes();
  };
  const aoPerderInternet = () => atualizarEstadoOffline({ online: false });
  const aoMostrarTela = () => document.visibilityState === "visible" && tentar();

  window.addEventListener("online", aoVoltarInternet);
  window.addEventListener("offline", aoPerderInternet);
  document.addEventListener("visibilitychange", aoMostrarTela);
  const intervalo = setInterval(tentar, 30000);

  atualizarContagem(authStorage.getFazendaId()).then(() => sincronizarPendentes());

  return () => {
    window.removeEventListener("online", aoVoltarInternet);
    window.removeEventListener("offline", aoPerderInternet);
    document.removeEventListener("visibilitychange", aoMostrarTela);
    clearInterval(intervalo);
  };
}

/** Baixa um arquivo da API (com o token) e salva no computador. */
async function baixar(path, nomeArquivo) {
  let res;
  try {
    res = await buscar(path);
  } catch {
    throw new ErroDeConexao("Sem internet: baixar arquivos precisa de conexão.");
  }
  if (res.status === 401) {
    authStorage.clear();
    window.location.reload();
    return;
  }
  if (!res.ok) {
    const corpo = await res.json().catch(() => ({}));
    throw new Error(corpo.erro || `Erro ${res.status} ao baixar o arquivo.`);
  }
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = nomeArquivo;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export const api = {
  baixar,
  get,
  post: (path, body, offline) => escrever("POST", path, body, offline),
  postForm: (path, formData) => request(path, { method: "POST", body: formData }),
  put: (path, body, offline) => escrever("PUT", path, body, offline),
  del,
};
