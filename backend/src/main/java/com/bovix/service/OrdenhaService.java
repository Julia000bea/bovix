package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.OrdenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ordenhas: registro por animal ou total do rebanho (sem animal).
 * O total do dia é a soma dos registros do dia; por isso um mesmo dia não pode ter registros
 * por animal e do rebanho ao mesmo tempo (a soma contaria o leite duas vezes).
 */
@Service
@RequiredArgsConstructor
public class OrdenhaService {

    static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdenhaRepository repository;
    private final AnimalRepository animalRepository;
    private final FazendaService fazendaService;
    private final Clock clock;

    public List<Ordenha> listarPorPeriodo(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim);
    }

    /**
     * Cria o registro. Com diasColeta = N > 1 (só para o total do rebanho), a data informada é o
     * último dia da coleta: o volume é dividido igualmente entre os N dias, marcados como estimados.
     * Devolve todos os registros criados.
     */
    @Transactional
    public List<Ordenha> criar(Long fazendaId, Long animalId, Ordenha dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        if (dados.getData().isAfter(LocalDate.now(clock))) {
            throw new RegraNegocioException("A data da ordenha não pode estar no futuro.");
        }
        int dias = dados.getDiasColeta() == null ? 1 : dados.getDiasColeta();

        Animal animal = null;
        if (animalId != null) {
            animal = animalRepository.findById(animalId)
                    .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
            if (!animal.getFazenda().getId().equals(fazendaId)) {
                throw new AccessDeniedException("Animal não pertence a esta fazenda.");
            }
            if (dias > 1) {
                throw new RegraNegocioException("Coleta de vários dias vale só para o total do rebanho, não por animal.");
            }
        }

        dados.setFazenda(fazenda);
        dados.setAnimal(animal);
        dados.setEstimado(false);
        dados.setColetaId(null);
        dados.setDiasColeta(dias);

        if (dias == 1) {
            verificarMistura(fazendaId, dados.getData(), animal != null);
            return List.of(repository.save(dados));
        }
        return List.copyOf(repository.saveAll(dividirColeta(fazenda, dados, dias)));
    }

    /** Um dia com total do rebanho não aceita registro por animal, e vice-versa. */
    void verificarMistura(Long fazendaId, LocalDate data, boolean porAnimal) {
        if (porAnimal && repository.existsByFazenda_IdAndDataAndAnimalIsNull(fazendaId, data)) {
            throw new RegraNegocioException("O dia " + BR.format(data)
                    + " já tem o total do rebanho registrado. Remova-o para lançar por animal.");
        }
        if (!porAnimal && repository.existsByFazenda_IdAndDataAndAnimalIsNotNull(fazendaId, data)) {
            throw new RegraNegocioException("O dia " + BR.format(data)
                    + " já tem ordenhas por animal. Remova-as para lançar o total do rebanho.");
        }
    }

    private List<Ordenha> dividirColeta(Fazenda fazenda, Ordenha dados, int dias) {
        LocalDate fim = dados.getData();
        LocalDate inicio = fim.minusDays(dias - 1L);
        repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazenda.getId(), inicio, fim).stream()
                .findFirst()
                .ifPresent(existente -> {
                    throw new RegraNegocioException("O dia " + BR.format(existente.getData())
                            + " já tem ordenha registrada. A coleta de " + dias + " dias não pode cobrir dias já lançados.");
                });

        // divide em partes iguais; o arredondamento sobra no último dia, para a soma bater
        BigDecimal total = dados.getLitros();
        BigDecimal parte = total.divide(BigDecimal.valueOf(dias), 2, RoundingMode.DOWN);
        BigDecimal ultimo = total.subtract(parte.multiply(BigDecimal.valueOf(dias - 1L)));
        String coletaId = UUID.randomUUID().toString();

        List<Ordenha> registros = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            registros.add(Ordenha.builder()
                    .fazenda(fazenda)
                    .data(inicio.plusDays(i))
                    .periodo("Dia todo")
                    .litros(i == dias - 1 ? ultimo : parte)
                    .estimado(true)
                    .diasColeta(dias)
                    .coletaId(coletaId)
                    .build());
        }
        return registros;
    }

    /** Remove o registro. Se ele faz parte de uma coleta de vários dias, remove a coleta inteira. */
    @Transactional
    public void remover(Long id) {
        Ordenha registro = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        Long fazendaId = registro.getFazenda().getId();
        fazendaService.verificarPropriedade(fazendaId);
        if (registro.getColetaId() != null) {
            repository.deleteAll(repository.findByFazenda_IdAndColetaId(fazendaId, registro.getColetaId()));
        } else {
            repository.delete(registro);
        }
    }
}
