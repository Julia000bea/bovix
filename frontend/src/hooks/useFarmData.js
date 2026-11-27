import { useCallback, useEffect, useRef, useState } from "react";
import { animaisService } from "../services/animaisService";
import { reproducaoService } from "../services/reproducaoService";
import { saudeService } from "../services/saudeService";
import { ordenhaService } from "../services/ordenhaService";
import { financeiroService } from "../services/financeiroService";
import { configService } from "../services/configService";
import { addDays, todayStr } from "../utils/date";
import { filaLocal, limparMarcaDadosSalvos } from "../services/offline";
import { authStorage, iniciarSincronizacaoAutomatica } from "../services/api";
import { useOffline } from "./useOffline";

/**
 * Carrega e mantém em memória todos os dados da fazenda vindos da API Java,
 * e expõe funções de escrita que já atualizam o estado local (otimista + refetch).
 *
 * Sem internet, as consultas devolvem a última cópia guardada no aparelho (ver api.js).
 * Quando a fila offline é enviada ou a API volta, recarrega em silêncio (sem a tela de carregamento).
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

  const jaCarregou = useRef(false);
  const offline = useOffline();

  const carregarTudo = useCallback(async ({ silencioso = false } = {}) => {
    if (!silencioso) setLoading(true);
    setErro(null);
    limparMarcaDadosSalvos();
    try {
      const inicioOrdenha = addDays(todayStr(), -60);
      const inicioFinanceiro = addDays(todayStr(), -400); // ~13 meses, para comparativos mensais

      const [a, r, s, o, f, c] = await Promise.all([
        animaisService.listar(),
        reproducaoService.listarPorFazenda(),
        saudeService.listarPorFazenda(),
        // a URL leva a data de hoje: a chave fixa deixa usar a cópia de ontem quando estiver offline
        ordenhaService.listarPorPeriodo(inicioOrdenha, todayStr(), { chaveCache: "ordenhas:recentes" }),
        financeiroService.listarPorPeriodo(inicioFinanceiro, todayStr(), { chaveCache: "financeiro:recentes" }),
        configService.buscar(),
      ]);

      // lançamentos feitos sem internet que ainda não foram enviados continuam aparecendo
      const pendentes = await registrosPendentes();
      setAnimais(a);
      setReproducao([...r, ...pendentes.reproducao]);
      setSaude([...s, ...pendentes.saude]);
      setOrdenha([...o, ...pendentes.ordenha]);
      setFinanceiro([...f, ...pendentes.financeiro]);
      setConfig(c);
      jaCarregou.current = true;
    } catch (e) {
      // recarga silenciosa que falhou: mantém o que já está na tela
      if (!(silencioso && jaCarregou.current)) setErro(e.message || "Não foi possível carregar os dados da fazenda.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    carregarTudo();
  }, [carregarTudo]);

  // envio automático da fila offline enquanto o sistema estiver aberto
  useEffect(() => iniciarSincronizacaoAutomatica(), []);

  // fila enviada / API de volta: troca os registros provisórios e a cópia guardada pelos dados atuais
  useEffect(() => {
    if (offline.versaoDados > 0) carregarTudo({ silencioso: true });
  }, [offline.versaoDados, carregarTudo]);

  return {
    animais, reproducao, saude, ordenha, financeiro, config,
    loading, erro, recarregar: () => carregarTudo(),
    setAnimais, setReproducao, setSaude, setOrdenha, setFinanceiro, setConfig,
  };
}

async function registrosPendentes() {
  const porColecao = { ordenha: [], reproducao: [], saude: [], financeiro: [] };
  const fila = await filaLocal.listar(authStorage.getFazendaId());
  for (const item of fila) {
    if (item.erro || !porColecao[item.colecao]) continue;
    porColecao[item.colecao].push(...(item.registros || []));
  }
  return porColecao;
}
