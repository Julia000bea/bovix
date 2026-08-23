package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.OrdenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenhaService {

    private final OrdenhaRepository repository;
    private final AnimalRepository animalRepository;
    private final FazendaService fazendaService;

    public List<Ordenha> listarPorPeriodo(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim);
    }

    public Ordenha criar(Long fazendaId, Long animalId, Ordenha dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        dados.setFazenda(fazenda);
        if (animalId != null) {
            Animal animal = animalRepository.findById(animalId)
                    .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
            if (!animal.getFazenda().getId().equals(fazendaId)) {
                throw new AccessDeniedException("Animal não pertence a esta fazenda.");
            }
            dados.setAnimal(animal);
        }
        return repository.save(dados);
    }

    public void remover(Long id) {
        Ordenha registro = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        fazendaService.verificarPropriedade(registro.getFazenda().getId());
        repository.deleteById(id);
    }
}
