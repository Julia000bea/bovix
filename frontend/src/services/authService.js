import { api, authStorage } from "./api";

export const authService = {
  async registrar({ nome, email, senha, fazendaNome, cidade, estado }) {
    const resp = await api.post("/auth/registrar", { nome, email, senha, fazendaNome, cidade, estado });
    aplicarSessao(resp);
    return resp;
  },
  async login({ email, senha }) {
    const resp = await api.post("/auth/login", { email, senha });
    aplicarSessao(resp);
    return resp;
  },
  async me() {
    const resp = await api.get("/auth/me");
    if (resp) aplicarSessao(resp, false);
    return resp;
  },
  logout() {
    authStorage.clear();
  },
};

function aplicarSessao(resp, definirToken = true) {
  if (!resp) return;
  if (definirToken) authStorage.setToken(resp.token);
  if (resp.fazendas && resp.fazendas.length > 0 && !authStorage.getFazendaId()) {
    authStorage.setFazendaId(resp.fazendas[0].id);
  }
}
