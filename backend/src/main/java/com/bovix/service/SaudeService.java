package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoSaude;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Registra o mesmo produto para todos os animais ativos da fazenda (ex.: vacina anual do rebanho).
     * Cria um registro por animal, para que carência e próxima aplicação continuem valendo por animal.
     */
    @Transactional
    public List<EventoSaude> criarParaRebanho(Long fazendaId, EventoSaude dados) {
        fazendaService.verificarPropriedade(fazendaId);
        List<Animal> ativos = animalRepository.findByFazenda_IdOrderByBrincoAsc(fazendaId).stream()
                .filter(a -> "Ativa".equals(a.getStatus()))
                .toList();
        if (ativos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum animal ativo na fazenda.");
        }
        List<EventoSaude> eventos = ativos.stream()
                .map(animal -> EventoSaude.builder()
                        .animal(animal)
                        .tipo(dados.getTipo())
                        .produto(dados.getProduto())
                        .data(dados.getData())
                        .carenciaLeiteDias(dados.getCarenciaLeiteDias())
                        .duracaoTratamentoDias(dados.getDuracaoTratamentoDias())
                        .proximaAplicacaoDias(dados.getProximaAplicacaoDias())
                        .observacoes(dados.getObservacoes())
                        .build())
                .toList();
        return repository.saveAll(eventos);
    }

    public void remover(Long id) {
        EventoSaude evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        animalService.verificarPropriedade(evento.getAnimal());
        repository.deleteById(id);
    }
}
