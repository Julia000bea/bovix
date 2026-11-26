// Cliente HTTP central: todas as chamadas à API Java passam por aqui.
const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();
if (import.meta.env.PROD && !configuredApiUrl) {
  throw new Error("VITE_API_URL não foi configurada no build de produção.");
}
const API_URL = (configuredApiUrl || "http://localhost:8080/api").replace(/\/+$/, "");

const TOKEN_KEY = "bovix_token";
const FAZENDA_KEY = "bovix_fazenda_id";

export const authStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(FAZENDA_KEY);
  },
  getFazendaId: () => Number(localStorage.getItem(FAZENDA_KEY)) || null,
  setFazendaId: (id) => localStorage.setItem(FAZENDA_KEY, String(id)),
};

export const FAZENDA_ID = () => authStorage.getFazendaId();

async function request(path, options = {}) {
  const token = authStorage.getToken();
  const headers = {
    ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
    ...options.headers,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  let res;
  try {
    res = await fetch(`${API_URL}${path}`, { ...options, headers });
  } catch (error) {
    throw new Error(
      `Não foi possível conectar à API (${API_URL}). Verifique VITE_API_URL, HTTPS e CORS.`,
      { cause: error },
    );
  }

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

/** Baixa um arquivo da API (com o token) e salva no computador. */
async function baixar(path, nomeArquivo) {
  const token = authStorage.getToken();
  let res;
  try {
    res = await fetch(`${API_URL}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  } catch (error) {
    throw new Error(`Não foi possível conectar à API (${API_URL}).`, { cause: error });
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
  get: (path) => request(path),
  post: (path, body) => request(path, { method: "POST", body: JSON.stringify(body) }),
  postForm: (path, formData) => request(path, { method: "POST", body: formData }),
  put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body) }),
  del: (path) => request(path, { method: "DELETE" }),
};
