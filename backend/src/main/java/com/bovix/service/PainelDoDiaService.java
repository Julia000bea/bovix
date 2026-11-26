package com.bovix.service;

import com.bovix.calculo.CicloReprodutivo;
import com.bovix.dto.AlertaDoDia;
import com.bovix.model.Animal;
import com.bovix.model.Configuracao;
import com.bovix.model.EventoReproducao;
import com.bovix.model.EventoSaude;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula o "Quadro do dia": previsão de parto, secagem e pré-parto, diagnóstico de prenhez
 * pendente, janelas de cio, carência do leite e reaplicação de vacina/medicamento.
 * É a única implementação desses alertas: a tela consome GET /fazendas/{id}/alertas e o
 * relatório por e-mail chama este mesmo service.
 *
 * Regra reprodutiva: depois do último parto, o evento de cio ou cobertura (inseminação/monta)
 * mais recente define em que fase o animal está. Se for uma cobertura, os eventos posteriores
 * (diagnóstico, prenhez confirmada, secagem, pré-parto) dizem se a gestação segue.
 */
@Service
@RequiredArgsConstructor
public class PainelDoDiaService {

    private static final int GESTACAO_PADRAO = 283;
    private static final int CIO_PADRAO = 21;
    private static final int PERIODO_SECO_PADRAO = 60;
    private static final int DIAS_PRE_PARTO_ANTES_DO_PARTO = 21;
    private static final int DIAS_PARA_DIAGNOSTICO = 30;

    private static final Map<String, Integer> ORDEM_URGENCIA = Map.of(
            "atrasado", 0, "urgente", 1, "ativo", 2, "proximo", 3
    );

    private final AnimalRepository animalRepository;
    private final EventoReproducaoRepository reproducaoRepository;
    private final EventoSaudeRepository saudeRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final FazendaService fazendaService;
    private final Clock clock;

    /** Para requisições do usuário: valida que a fazenda é dele. */
    public List<AlertaDoDia> calcularAlertas(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return calcularAlertasDaFazenda(fazendaId);
    }

    /**
     * Sem checagem de usuário — usado pelo agendador do e-mail diário, que roda fora de
     * qualquer requisição autenticada. Não exponha em controller.
     */
    public List<AlertaDoDia> calcularAlertasDaFazenda(Long fazendaId) {
        LocalDate hoje = LocalDate.now(clock);
        Configuracao config = configuracaoRepository.findByFazenda_Id(fazendaId).orElse(null);
        int gestacaoDias = config != null && config.getGestacaoDias() != null ? config.getGestacaoDias() : GESTACAO_PADRAO;
        int cioDias = config != null && config.getCioDias() != null ? config.getCioDias() : CIO_PADRAO;
        int periodoSecoDias = config != null && config.getPeriodoSecoDias() != null ? config.getPeriodoSecoDias() : PERIODO_SECO_PADRAO;

        List<Animal> ativos = animalRepository.findByFazenda_IdOrderByBrincoAsc(fazendaId).stream()
                .filter(a -> "Ativa".equals(a.getStatus()))
                .toList();

        Map<Long, List<EventoReproducao>> reproPorAnimal = reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId)
                .stream().collect(Collectors.groupingBy(EventoReproducao::getAnimalId));
        Map<Long, List<EventoSaude>> saudePorAnimal = saudeRepository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId)
                .stream().collect(Collectors.groupingBy(EventoSaude::getAnimalId));

        List<AlertaDoDia> alertas = new ArrayList<>();
        for (Animal animal : ativos) {
            List<EventoReproducao> repro = reproPorAnimal.getOrDefault(animal.getId(), List.of());
            alertasReprodutivos(animal, repro, hoje, gestacaoDias, cioDias, periodoSecoDias, alertas);
            alertasSaude(animal, saudePorAnimal.getOrDefault(animal.getId(), List.of()), hoje, alertas);
        }

        alertas.sort(Comparator
                .comparingInt((AlertaDoDia a) -> ORDEM_URGENCIA.getOrDefault(a.getUrgencia(), 9))
                .thenComparing(AlertaDoDia::getDias, Comparator.nullsLast(Comparator.naturalOrder())));
        return alertas;
    }

    // ------------------------------------------------------------------ reprodução

    private void alertasReprodutivos(Animal animal, List<EventoReproducao> eventos, LocalDate hoje,
                                     int gestacaoDias, int cioDias, int periodoSecoDias, List<AlertaDoDia> alertas) {
        CicloReprodutivo.Estado estado = CicloReprodutivo.estado(eventos, hoje, gestacaoDias);

        switch (estado.situacao()) {
            case SEM_COBERTURA -> {
                return;
            }
            case CIO -> {
                LocalDate proximoCio = estado.marco().getData().plusDays(cioDias);
                alertaJanelaCio(animal, "cio", proximoCio, hoje, "Janela de cio prevista — " + formatar(proximoCio), alertas);
                return;
            }
            case VAZIA -> {
                EventoReproducao fim = estado.encerramento();
                LocalDate proximoCio = fim.getData().plusDays(cioDias);
                String motivo = "perda_gestacional".equals(fim.getTipo()) ? "perda gestacional" : "diagnóstico: vazia";
                alertaJanelaCio(animal, "retorno", proximoCio, hoje,
                        "Possível retorno de cio (" + motivo + ") — janela " + formatar(proximoCio), alertas);
                return;
            }
            default -> { /* COBERTA ou PRENHE: segue abaixo */ }
        }

        EventoReproducao cobertura = estado.cobertura();
        LocalDate previsao = estado.previsaoParto();
        long diasParto = ChronoUnit.DAYS.between(hoje, previsao);
        if (diasParto <= 15 && diasParto >= -10) {
            alertas.add(alerta("parto-" + animal.getId(), animal, "parto", urgenciaParto(diasParto), diasParto,
                    textoParto(diasParto, previsao)));
        }

        if (estado.situacao() == CicloReprodutivo.Situacao.PRENHE) {
            if (!estado.secou() && diasParto > 0) {
                LocalDate dataSecagem = previsao.minusDays(periodoSecoDias);
                long dias = ChronoUnit.DAYS.between(hoje, dataSecagem);
                if (dias <= 7) {
                    alertas.add(alerta("secagem-" + animal.getId(), animal, "secagem", urgenciaPrazo(dias), dias,
                            textoPrazo("Secagem", dias, dataSecagem) + " (parto previsto " + formatar(previsao) + ")"));
                }
            }
            if (!estado.entrouPreParto() && diasParto > 0) {
                LocalDate dataPreParto = previsao.minusDays(DIAS_PRE_PARTO_ANTES_DO_PARTO);
                long dias = ChronoUnit.DAYS.between(hoje, dataPreParto);
                if (dias <= 7) {
                    alertas.add(alerta("preparto-" + animal.getId(), animal, "preparto", urgenciaPrazo(dias), dias,
                            textoPrazo("Entrada no pré-parto", dias, dataPreParto)));
                }
            }
            return;
        }

        // cobertura ainda sem diagnóstico: observar retorno de cio e cobrar o diagnóstico
        LocalDate retorno = cobertura.getData().plusDays(cioDias);
        alertaJanelaCio(animal, "retorno", retorno, hoje,
                "Observar retorno de cio após a cobertura — janela " + formatar(retorno), alertas);

        long diasDesdeCobertura = ChronoUnit.DAYS.between(cobertura.getData(), hoje);
        if (diasDesdeCobertura >= DIAS_PARA_DIAGNOSTICO && diasParto > 15) {
            String urgencia = diasDesdeCobertura > 60 ? "atrasado" : diasDesdeCobertura >= 45 ? "urgente" : "proximo";
            alertas.add(alerta("diagnostico-" + animal.getId(), animal, "diagnostico", urgencia,
                    DIAS_PARA_DIAGNOSTICO - diasDesdeCobertura,
                    "Diagnóstico de prenhez pendente — " + diasDesdeCobertura + " dias desde a cobertura de "
                            + formatar(cobertura.getData())));
        }
    }

    private void alertaJanelaCio(Animal animal, String prefixo, LocalDate data, LocalDate hoje, String texto,
                                 List<AlertaDoDia> alertas) {
        long dias = ChronoUnit.DAYS.between(hoje, data);
        if (dias >= -3 && dias <= 3) {
            alertas.add(alerta(prefixo + "-" + animal.getId(), animal, "cio", dias <= 0 ? "urgente" : "proximo", dias, texto));
        }
    }

    // ------------------------------------------------------------------ saúde

    private void alertasSaude(Animal animal, List<EventoSaude> eventos, LocalDate hoje, List<AlertaDoDia> alertas) {
        for (EventoSaude e : eventos) {
            // a carência conta a partir da ÚLTIMA aplicação do tratamento (ver EventoSaude.getFimCarencia)
            LocalDate fim = e.getFimCarencia();
            if (fim != null && !hoje.isAfter(fim)) {
                LocalDate ultimaAplicacao = e.getDataUltimaAplicacao();
                String tratamento = ultimaAplicacao.isAfter(e.getData())
                        ? "; tratamento até " + formatar(ultimaAplicacao)
                        : "";
                alertas.add(alerta("carencia-" + e.getId(), animal, "carencia", "ativo",
                        ChronoUnit.DAYS.between(hoje, fim),
                        "Leite em carência até " + formatar(fim) + " — descartar o leite (" + e.getProduto() + tratamento + ")"));
            }
        }

        // Reaplicação: só a aplicação mais recente de cada produto conta — uma dose nova encerra o alerta da anterior.
        Map<String, EventoSaude> ultimaPorProduto = new LinkedHashMap<>();
        eventos.stream()
                .sorted(Comparator.comparing(EventoSaude::getData)
                        .thenComparing(EventoSaude::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed())
                .forEach(e -> ultimaPorProduto.putIfAbsent(chaveProduto(e), e));

        for (EventoSaude e : ultimaPorProduto.values()) {
            if (e.getProximaAplicacaoDias() == null || e.getProximaAplicacaoDias() <= 0) continue;
            LocalDate prox = e.getData().plusDays(e.getProximaAplicacaoDias());
            long dias = ChronoUnit.DAYS.between(hoje, prox);
            if (dias <= 15) {
                String texto = dias < 0
                        ? e.getProduto() + ": reaplicação atrasada (" + (-dias) + "d)"
                        : e.getProduto() + ": próxima aplicação em " + dias + " dia(s)";
                alertas.add(alerta("reaplicacao-" + e.getId(), animal, "saude", urgenciaPrazo(dias), dias, texto));
            }
        }
    }

    private String chaveProduto(EventoSaude e) {
        return e.getTipo() + "|" + (e.getProduto() == null ? "" : e.getProduto().trim().toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ utilitários

    private AlertaDoDia alerta(String id, Animal animal, String tipo, String urgencia, Long dias, String texto) {
        return new AlertaDoDia(id, animal.getId(), animal.getBrinco(), animal.getNome(), tipo, urgencia, dias, texto);
    }

    private String urgenciaParto(long dias) {
        if (dias < 0) return "atrasado";
        if (dias <= 7) return "urgente";
        return "proximo";
    }

    private String urgenciaPrazo(long dias) {
        if (dias < 0) return "atrasado";
        if (dias <= 3) return "urgente";
        return "proximo";
    }

    private String textoParto(long dias, LocalDate previsao) {
        if (dias < 0) return "Parto previsto há " + (-dias) + " dia(s) — " + formatar(previsao);
        if (dias == 0) return "Parto previsto para hoje";
        return "Parto previsto em " + dias + " dia(s) — " + formatar(previsao);
    }

    private String textoPrazo(String acao, long dias, LocalDate data) {
        if (dias < 0) return acao + " atrasada há " + (-dias) + " dia(s) — prevista para " + formatar(data);
        if (dias == 0) return acao + " prevista para hoje";
        return acao + " prevista em " + dias + " dia(s) — " + formatar(data);
    }

    private String formatar(LocalDate data) {
        return String.format("%02d/%02d/%04d", data.getDayOfMonth(), data.getMonthValue(), data.getYear());
    }
}
