package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoSaude;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaudeService {

    private final EventoSaudeRepository repository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final FazendaService fazendaService;

    public List<EventoSaude> listarPorFazenda(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);
    }

    public EventoSaude criar(Long animalId, EventoSaude dados) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
        animalService.verificarPropriedade(animal);
        dados.setAnimal(animal);
        return repository.save(dados);
    }

    public void remover(Long id) {
        EventoSaude evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        animalService.verificarPropriedade(evento.getAnimal());
        repository.deleteById(id);
    }
}
