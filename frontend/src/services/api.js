// Cliente HTTP central: todas as chamadas à API Java passam por aqui.
const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

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
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
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

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: "POST", body: JSON.stringify(body) }),
  postForm: (path, formData) => request(path, { method: "POST", body: formData }),
  put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body) }),
  del: (path) => request(path, { method: "DELETE" }),
};
