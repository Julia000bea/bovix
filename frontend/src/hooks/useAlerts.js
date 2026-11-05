import { useEffect, useMemo, useState } from "react";
import { painelService } from "../services/painelService";

/**
 * Quadro do dia: os alertas (parto, secagem, pré-parto, diagnóstico pendente, cio, carência
 * do leite e reaplicação) são calculados só no backend — a mesma regra do e-mail diário.
 * Recarrega sempre que os dados da fazenda mudam (novo evento, animal ou configuração).
 */
export function useAlerts({ animais, reproducao, saude, config }) {
  const [alertas, setAlertas] = useState([]);

  useEffect(() => {
    let ativo = true;
    painelService
      .alertas()
      .then((lista) => {
        if (ativo) setAlertas(lista || []);
      })
      .catch((e) => console.error("Falha ao carregar o quadro do dia:", e));
    return () => {
      ativo = false;
    };
  }, [animais, reproducao, saude, config]);

  return useMemo(
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
}
