package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReproducaoService {

    private final EventoReproducaoRepository repository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final FazendaService fazendaService;
    private final ConfiguracaoService configuracaoService;

    public List<EventoReproducao> listarPorFazenda(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);
    }

    public EventoReproducao criar(Long animalId, EventoReproducao dados) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
        animalService.verificarPropriedade(animal);
        dados.setAnimal(animal);

        LocalDate dataConcepcao = dataDaConcepcao(animal.getId(), dados);
        if (dataConcepcao != null) {
            Integer diasConfigurados = configuracaoService.buscarOuCriar(animal.getFazenda().getId()).getGestacaoDias();
            int gestacaoDias = diasConfigurados != null ? diasConfigurados : 283;
            dados.setPrevisaoParto(dataConcepcao.plusDays(gestacaoDias));
        }

        if ("parto".equals(dados.getTipo())) {
            animal.setCategoria("Vaca lactante");
            animalRepository.save(animal);
        } else if ("secagem".equals(dados.getTipo())) {
            animal.setCategoria("Vaca seca");
            animalRepository.save(animal);
        }

        return repository.save(dados);
    }

    private LocalDate dataDaConcepcao(Long animalId, EventoReproducao evento) {
        if ("inseminacao".equals(evento.getTipo()) || "reproducao".equals(evento.getTipo())) {
            return evento.getData();
        }

        if (!"prenhez_confirmada".equals(evento.getTipo())) {
            return null;
        }

        return repository.findByAnimal_IdAndTipoInOrderByDataDesc(animalId, Set.of("inseminacao", "reproducao"))
                .stream()
                .map(EventoReproducao::getData)
                .filter(data -> !data.isAfter(evento.getData()))
                .findFirst()
                .orElse(null);
    }

    public EventoReproducao confirmarCio(Long eventoId) {
        EventoReproducao evento = repository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + eventoId));
        animalService.verificarPropriedade(evento.getAnimal());

        if (!"cio_detectado".equals(evento.getTipo())) {
            throw new IllegalArgumentException("Apenas eventos de cio detectado podem ser confirmados");
        }

        evento.setDataConfirmacao(LocalDate.now());
        return repository.save(evento);
    }

    public void remover(Long id) {
        EventoReproducao evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        animalService.verificarPropriedade(evento.getAnimal());
        repository.deleteById(id);
    }
}
