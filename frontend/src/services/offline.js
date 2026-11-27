// Armazenamento offline do Bovix (IndexedDB, dentro do navegador/aparelho).
//
// Guarda duas coisas:
//  - "cache": a última resposta de cada consulta (GET) à API, para o sistema abrir sem internet;
//  - "fila":  os lançamentos feitos sem internet (ordenha, reprodução, saúde, financeiro),
//             que são enviados à API, na ordem, assim que a conexão volta (ver api.js).
//
// Também mantém um pequeno "estado" (online?, quantos pendentes, falhas...) que a tela
// acompanha pelo hook useOffline.

const DB_NOME = "bovix-offline";
const DB_VERSAO = 1;

let dbPromessa = null;

function abrirBanco() {
  if (dbPromessa) return dbPromessa;
  dbPromessa = new Promise((resolve, reject) => {
    if (typeof indexedDB === "undefined") {
      reject(new Error("IndexedDB indisponível neste navegador."));
      return;
    }
    const req = indexedDB.open(DB_NOME, DB_VERSAO);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains("cache")) db.createObjectStore("cache", { keyPath: "chave" });
      if (!db.objectStoreNames.contains("fila")) db.createObjectStore("fila", { keyPath: "localId" });
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
  dbPromessa.catch(() => {
    dbPromessa = null;
  });
  return dbPromessa;
}

async function operar(store, modo, fn) {
  const db = await abrirBanco();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, modo);
    const resultado = fn(tx.objectStore(store));
    tx.oncomplete = () => resolve(resultado && "result" in resultado ? resultado.result : undefined);
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
}

// ---------------------------------------------------------------- cache das consultas

export const cacheLocal = {
  async salvar(chave, dados) {
    try {
      await operar("cache", "readwrite", (s) => s.put({ chave, dados, salvoEm: Date.now() }));
    } catch (e) {
      console.warn("Bovix offline: não foi possível guardar no aparelho.", e);
    }
  },
  async ler(chave) {
    try {
      return (await operar("cache", "readonly", (s) => s.get(chave))) || null;
    } catch {
      return null;
    }
  },
  async limpar() {
    try {
      await operar("cache", "readwrite", (s) => s.clear());
    } catch {
      /* sem IndexedDB: nada a limpar */
    }
  },
};

// ---------------------------------------------------------------- fila de lançamentos

let sequencia = 0;

export const filaLocal = {
  /**
   * Guarda um lançamento para enviar depois. Devolve o item, com o localId provisório.
   * `colecao` + `montarRegistros(localId)`: o que a tela mostra até o envio (e volta a mostrar
   * se o app for fechado e aberto de novo antes de a fila ser enviada).
   */
  async adicionar({ method, path, body, descricao, fazendaId, colecao, montarRegistros }) {
    const agora = Date.now();
    const localId = `local-${agora.toString(36)}-${(sequencia++).toString(36)}${Math.random().toString(36).slice(2, 6)}`;
    const registros = montarRegistros ? montarRegistros(localId) : null;
    const item = {
      localId,
      method,
      path,
      body,
      descricao,
      fazendaId,
      colecao: colecao || null,
      registros: Array.isArray(registros) ? registros : registros ? [registros] : [],
      resposta: registros,
      criadoEm: agora,
      erro: null,
    };
    await operar("fila", "readwrite", (s) => s.put(item));
    await atualizarContagem(fazendaId);
    return item;
  },
  /** Lançamentos desta fazenda, do mais antigo para o mais novo. */
  async listar(fazendaId) {
    try {
      const todos = (await operar("fila", "readonly", (s) => s.getAll())) || [];
      return todos.filter((i) => i.fazendaId === fazendaId).sort((a, b) => a.criadoEm - b.criadoEm);
    } catch {
      return [];
    }
  },
  async remover(localId, fazendaId) {
    await operar("fila", "readwrite", (s) => s.delete(localId));
    await atualizarContagem(fazendaId);
  },
  async marcarErro(item, erro) {
    await operar("fila", "readwrite", (s) => s.put({ ...item, erro }));
    await atualizarContagem(item.fazendaId);
  },
  async limparErro(localId, fazendaId) {
    const lista = await filaLocal.listar(fazendaId);
    const item = lista.find((i) => i.localId === localId);
    if (item) await operar("fila", "readwrite", (s) => s.put({ ...item, erro: null }));
    await atualizarContagem(fazendaId);
  },
};

// ---------------------------------------------------------------- estado para a tela

let estado = {
  online: typeof navigator === "undefined" ? true : navigator.onLine,
  pendentes: 0, // lançamentos aguardando envio (sem erro)
  falhas: [], // lançamentos que a API recusou (com a mensagem de erro)
  sincronizando: false,
  sessaoExpirada: false,
  dadosSalvosEm: null, // quando a tela mostra dados guardados no aparelho: a data deles
  versaoDados: 0, // muda quando é preciso recarregar os dados da API (após sincronizar)
};
const ouvintes = new Set();

export function lerEstadoOffline() {
  return estado;
}

export function assinarEstadoOffline(fn) {
  ouvintes.add(fn);
  return () => ouvintes.delete(fn);
}

export function atualizarEstadoOffline(parcial) {
  const novo = { ...estado, ...parcial };
  const mudou = Object.keys(parcial).some((k) => novo[k] !== estado[k]);
  if (!mudou) return;
  estado = novo;
  ouvintes.forEach((fn) => fn());
}

export async function atualizarContagem(fazendaId) {
  const lista = await filaLocal.listar(fazendaId);
  atualizarEstadoOffline({
    pendentes: lista.filter((i) => !i.erro).length,
    falhas: lista.filter((i) => i.erro),
  });
}

/** A tela está mostrando dados guardados no aparelho (não vieram da API agora). */
export function marcarDadosSalvos(salvoEm) {
  const atual = estado.dadosSalvosEm;
  atualizarEstadoOffline({ dadosSalvosEm: atual ? Math.min(atual, salvoEm) : salvoEm });
}

export function limparMarcaDadosSalvos() {
  atualizarEstadoOffline({ dadosSalvosEm: null });
}

export function pedirRecargaDosDados() {
  atualizarEstadoOffline({ versaoDados: estado.versaoDados + 1 });
}
