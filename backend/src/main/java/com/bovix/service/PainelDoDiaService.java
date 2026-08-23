package com.bovix.service;

import com.bovix.dto.AlertaDoDia;
import com.bovix.model.Animal;
import com.bovix.model.Configuracao;
import com.bovix.model.EventoReproducao;
import com.bovix.model.EventoSaude;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Calcula o "Quadro do dia" no servidor — é o mesmo algoritmo do hook useAlerts.js do frontend
 * (previsão de parto/cio a partir do último evento reprodutivo, carência de leite, reaplicação
 * de vacina/medicamento). Precisa existir aqui também porque o relatório por e-mail roda sozinho,
 * sem ninguém com o navegador aberto.
 */
@Service
@RequiredArgsConstructor
public class PainelDoDiaService {

    private static final Map<String, Integer> ORDEM_URGENCIA = Map.of(
            "atrasado", 0, "urgente", 1, "ativo", 2, "proximo", 3
    );

    private final AnimalRepository animalRepository;
    private final EventoReproducaoRepository reproducaoRepository;
    private final EventoSaudeRepository saudeRepository;
    private final ConfiguracaoService configuracaoService;

    public List<AlertaDoDia> calcularAlertas(Long fazendaId) {
        LocalDate hoje = LocalDate.now();
        Configuracao config = configuracaoService.buscarOuCriar(fazendaId);
        int gestacaoDias = config.getGestacaoDias() != null ? config.getGestacaoDias() : 283;
        int cioDias = config.getCioDias() != null ? config.getCioDias() : 21;

        List<Animal> ativos = animalRepository.findByFazenda_IdOrderByBrincoAsc(fazendaId).stream()
                .filter(a -> "Ativa".equals(a.getStatus()))
                .toList();

        List<EventoReproducao> todaReproducao = reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);
        List<EventoSaude> todaSaude = saudeRepository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);

        List<AlertaDoDia> alertas = new ArrayList<>();

        for (Animal animal : ativos) {
            List<EventoReproducao> repro = todaReproducao.stream()
                    .filter(e -> animal.getId().equals(e.getAnimalId()))
                    .sorted(Comparator.comparing(EventoReproducao::getData).reversed())
                    .toList();

            EventoReproducao ultimo = repro.isEmpty() ? null : repro.get(0);

            if (ultimo != null && "inseminacao".equals(ultimo.getTipo())) {
                boolean negDepois = repro.stream().anyMatch(e -> "diagnostico".equals(e.getTipo())
                        && "negativo".equals(e.getResultado()) && e.getData().isAfter(ultimo.getData()));
                boolean partoDepois = repro.stream().anyMatch(e -> "parto".equals(e.getTipo())
                        && e.getData().isAfter(ultimo.getData()));

                if (!negDepois && !partoDepois) {
                    LocalDate previsao = ultimo.getData().plusDays(gestacaoDias);
                    long dias = ChronoUnit.DAYS.between(hoje, previsao);
                    if (dias <= 15 && dias >= -10) {
                        alertas.add(new AlertaDoDia(animal.getBrinco(), animal.getNome(),
                                urgenciaParto(dias), textoParto(dias, previsao)));
                    }
                } else if (negDepois) {
                    EventoReproducao cioBase = repro.stream()
                            .filter(e -> "diagnostico".equals(e.getTipo()) && "negativo".equals(e.getResultado()))
                            .findFirst().orElse(null);
                    if (cioBase != null) {
                        LocalDate proximoCio = cioBase.getData().plusDays(cioDias);
                        long dias = ChronoUnit.DAYS.between(hoje, proximoCio);
                        if (dias >= -3 && dias <= 3) {
                            alertas.add(new AlertaDoDia(animal.getBrinco(), animal.getNome(),
                                    dias <= 0 ? "urgente" : "proximo",
                                    "Possível retorno de cio (diagnóstico negativo) — janela " + formatar(proximoCio)));
                        }
                    }
                }
            }

            if (ultimo != null && "cio".equals(ultimo.getTipo())) {
                LocalDate proximoCio = ultimo.getData().plusDays(cioDias);
                long dias = ChronoUnit.DAYS.between(hoje, proximoCio);
                if (dias >= -3 && dias <= 3) {
                    alertas.add(new AlertaDoDia(animal.getBrinco(), animal.getNome(),
                            dias <= 0 ? "urgente" : "proximo",
                            "Janela de cio prevista — " + formatar(proximoCio)));
                }
            }

            List<EventoSaude> saudeAnimal = todaSaude.stream()
                    .filter(e -> animal.getId().equals(e.getAnimalId()))
                    .toList();

            for (EventoSaude e : saudeAnimal) {
                if (e.getCarenciaLeiteDias() != null) {
                    LocalDate fim = e.getData().plusDays(e.getCarenciaLeiteDias());
                    if (!hoje.isAfter(fim)) {
                        alertas.add(new AlertaDoDia(animal.getBrinco(), animal.getNome(), "ativo",
                                "Leite em carência até " + formatar(fim) + " (" + e.getProduto() + ")"));
                    }
                }
                if (e.getProximaAplicacaoDias() != null) {
                    LocalDate prox = e.getData().plusDays(e.getProximaAplicacaoDias());
                    long dias = ChronoUnit.DAYS.between(hoje, prox);
                    if (dias <= 15) {
                        String texto = dias < 0
                                ? e.getProduto() + ": reaplicação atrasada (" + (-dias) + "d)"
                                : e.getProduto() + ": próxima aplicação em " + dias + " dia(s)";
                        alertas.add(new AlertaDoDia(animal.getBrinco(), animal.getNome(),
                                dias < 0 ? "atrasado" : dias <= 3 ? "urgente" : "proximo", texto));
                    }
                }
            }
        }

        alertas.sort(Comparator.comparingInt(a -> ORDEM_URGENCIA.getOrDefault(a.getUrgencia(), 9)));
        return alertas;
    }

    private String urgenciaParto(long dias) {
        if (dias < 0) return "atrasado";
        if (dias <= 7) return "urgente";
        return "proximo";
    }

    private String textoParto(long dias, LocalDate previsao) {
        if (dias < 0) return "Parto previsto há " + (-dias) + " dia(s) — " + formatar(previsao);
        if (dias == 0) return "Parto previsto para hoje";
        return "Parto previsto em " + dias + " dia(s) — " + formatar(previsao);
    }

    private String formatar(LocalDate data) {
        return String.format("%02d/%02d/%04d", data.getDayOfMonth(), data.getMonthValue(), data.getYear());
    }
}
