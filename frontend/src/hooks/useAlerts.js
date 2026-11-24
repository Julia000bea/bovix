import { useCallback, useEffect, useMemo, useState } from "react";
import { painelService } from "../services/painelService";

/**
 * Quadro do dia: os alertas (parto, secagem, pré-parto, diagnóstico pendente, cio, carência
 * do leite e reaplicação) são calculados só no backend — a mesma regra do e-mail diário.
 * Recarrega sempre que os dados da fazenda mudam (novo evento, animal ou configuração).
 *
 * Expõe carregando/erro: se a chamada falhar, a tela precisa dizer isso — mostrar
 * "tudo em dia" esconderia, por exemplo, uma vaca com leite em carência.
 */
export function useAlerts({ animais, reproducao, saude, config }) {
  const [alertas, setAlertas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    painelService
      .alertas()
      .then((lista) => {
        if (!ativo) return;
        setAlertas(lista || []);
        setErro(null);
      })
      .catch((e) => {
        console.error("Falha ao carregar o quadro do dia:", e);
        if (ativo) setErro(e.message || "Não foi possível carregar o quadro do dia.");
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [animais, reproducao, saude, config, tentativa]);

  const recarregar = useCallback(() => setTentativa((t) => t + 1), []);

  const lista = useMemo(
    () =>
      alertas.map((a) => ({
        id: a.id,
        animal: animais.find((x) => x.id === a.animalId) || { id: a.animalId, brinco: a.animalBrinco, nome: a.animalNome },
        dias: a.dias,
        urgencia: a.urgencia,
        icone: a.tipo,
        texto: a.texto,
      })),
    [alertas, animais]
  );

  return { alertas: lista, carregando, erro, recarregar };
}
