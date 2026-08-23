package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReproducaoService {

    private final EventoReproducaoRepository repository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final FazendaService fazendaService;

    public List<EventoReproducao> listarPorFazenda(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);
    }

    public EventoReproducao criar(Long animalId, EventoReproducao dados) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
        animalService.verificarPropriedade(animal);
        dados.setAnimal(animal);

        if ("parto".equals(dados.getTipo())) {
            animal.setCategoria("Vaca lactante");
            animalRepository.save(animal);
        } else if ("secagem".equals(dados.getTipo())) {
            animal.setCategoria("Vaca seca");
            animalRepository.save(animal);
        }

        return repository.save(dados);
    }

    public void remover(Long id) {
        EventoReproducao evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        animalService.verificarPropriedade(evento.getAnimal());
        repository.deleteById(id);
    }
}
