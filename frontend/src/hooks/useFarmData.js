import { useCallback, useEffect, useState } from "react";
import { animaisService } from "../services/animaisService";
import { reproducaoService } from "../services/reproducaoService";
import { saudeService } from "../services/saudeService";
import { ordenhaService } from "../services/ordenhaService";
import { financeiroService } from "../services/financeiroService";
import { configService } from "../services/configService";
import { addDays, todayStr } from "../utils/date";

/**
 * Carrega e mantém em memória todos os dados da fazenda vindos da API Java,
 * e expõe funções de escrita que já atualizam o estado local (otimista + refetch).
 */
export function useFarmData() {
  const [animais, setAnimais] = useState([]);
  const [reproducao, setReproducao] = useState([]);
  const [saude, setSaude] = useState([]);
  const [ordenha, setOrdenha] = useState([]);
  const [financeiro, setFinanceiro] = useState([]);
  const [config, setConfig] = useState({ gestacaoDias: 283, cioDias: 21, notificacaoAtiva: false, notificacaoEmail: "", notificacaoHorario: "07:00:00" });
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);

  const carregarTudo = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const inicioOrdenha = addDays(todayStr(), -60);
      const inicioFinanceiro = addDays(todayStr(), -400); // ~13 meses, para comparativos mensais

      const [a, r, s, o, f, c] = await Promise.all([
        animaisService.listar(),
        reproducaoService.listarPorFazenda(),
        saudeService.listarPorFazenda(),
        ordenhaService.listarPorPeriodo(inicioOrdenha, todayStr()),
        financeiroService.listarPorPeriodo(inicioFinanceiro, todayStr()),
        configService.buscar(),
      ]);

      setAnimais(a);
      setReproducao(r);
      setSaude(s);
      setOrdenha(o);
      setFinanceiro(f);
      setConfig(c);
    } catch (e) {
      setErro(e.message || "Não foi possível carregar os dados da fazenda.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    carregarTudo();
  }, [carregarTudo]);

  return {
    animais, reproducao, saude, ordenha, financeiro, config,
    loading, erro, recarregar: carregarTudo,
    setAnimais, setReproducao, setSaude, setOrdenha, setFinanceiro, setConfig,
  };
}
