import { useCallback, useEffect, useState } from "react";
import { authService } from "../services/authService";
import { authStorage } from "../services/api";

export function useAuth() {
  const [usuario, setUsuario] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);

  const restaurarSessao = useCallback(async () => {
    // abriu o link de "esqueci minha senha": sai da sessão atual para mostrar a tela de nova senha
    if ((window.location.hash || "").startsWith("#redefinir=")) {
      authStorage.clear();
    }
    if (!authStorage.getToken()) {
      setCarregando(false);
      return;
    }
    try {
      const resp = await authService.me();
      if (resp) setUsuario(resp.usuario);
    } catch {
      authStorage.clear();
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    restaurarSessao();
  }, [restaurarSessao]);

  const login = async (email, senha) => {
    setErro(null);
    try {
      const resp = await authService.login({ email, senha });
      setUsuario(resp.usuario);
      return true;
    } catch (e) {
      setErro(e.message || "Não foi possível entrar.");
      return false;
    }
  };

  const registrar = async (dados) => {
    setErro(null);
    try {
      const resp = await authService.registrar(dados);
      setUsuario(resp.usuario);
      return true;
    } catch (e) {
      setErro(e.message || "Não foi possível criar a conta.");
      return false;
    }
  };

  const logout = () => {
    authService.logout();
    setUsuario(null);
  };

  return { usuario, autenticado: !!usuario, carregando, erro, login, registrar, logout };
}
