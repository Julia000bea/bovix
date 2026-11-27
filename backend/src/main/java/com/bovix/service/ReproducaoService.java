package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReproducaoService {

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "cio_detectado", "cio_confirmado", "inseminacao", "reproducao",
            "diagnostico_prenhez", "prenhez_confirmada", "secagem", "pre_parto", "parto", "perda_gestacional");
    private static final Set<String> TIPOS_COBERTURA = Set.of("inseminacao", "reproducao");
    private static final Set<String> TIPOS_CIO = Set.of("cio_detectado", "cio_confirmado");
    // eventos que definem a categoria do animal (lactante x seca)
    private static final Set<String> TIPOS_CATEGORIA = Set.of("parto", "secagem", "pre_parto");

    private final EventoReproducaoRepository repository;
    private final AnimalRepository animalRepository;
    private final AnimalService animalService;
    private final FazendaService fazendaService;
    private final ConfiguracaoService configuracaoService;
    private final Clock clock;

    public List<EventoReproducao> listarPorFazenda(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByAnimal_Fazenda_IdOrderByDataDesc(fazendaId);
    }

    @Transactional
    public EventoReproducao criar(Long animalId, EventoReproducao dados) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + animalId));
        animalService.verificarPropriedade(animal);
        if (animal.ehMacho()) {
            throw new RegraNegocioException("Eventos de reprodução (cio, cobertura, prenhez, parto) são só para fêmeas.");
        }
        validar(dados);
        dados.setAnimal(animal);
        // campos controlados pelo servidor
        dados.setCriadoEm(null);
        dados.setDataConfirmacao(null);
        vincularCio(animal, dados);

        LocalDate dataConcepcao = dataDaConcepcao(animal.getId(), dados);
        if (dataConcepcao != null) {
            Integer diasConfigurados = configuracaoService.buscarOuCriar(animal.getFazenda().getId()).getGestacaoDias();
            int gestacaoDias = diasConfigurados != null ? diasConfigurados : 283;
            dados.setPrevisaoParto(dataConcepcao.plusDays(gestacaoDias));
        } else if (!"prenhez_confirmada".equals(dados.getTipo())) {
            // só a prenhez confirmada sem cobertura registrada aceita previsão informada à mão
            dados.setPrevisaoParto(null);
        }

        EventoReproducao salvo = repository.save(dados);
        if (TIPOS_CATEGORIA.contains(salvo.getTipo())) {
            atualizarCategoria(animal);
        }
        return salvo;
    }

    private void validar(EventoReproducao dados) {
        if (!TIPOS_VALIDOS.contains(dados.getTipo())) {
            throw new RegraNegocioException("Tipo de evento reprodutivo inválido: " + dados.getTipo());
        }
        if (dados.getData().isAfter(LocalDate.now(clock))) {
            throw new RegraNegocioException("A data do evento não pode estar no futuro.");
        }
        if ("diagnostico_prenhez".equals(dados.getTipo())) {
            if (!"prenhe".equals(dados.getResultado()) && !"vazia".equals(dados.getResultado())) {
                throw new RegraNegocioException("Informe o resultado do diagnóstico: prenhe ou vazia.");
            }
        } else {
            dados.setResultado(null); // resultado só faz sentido no diagnóstico
        }
    }

    /** Liga a cobertura ao cio de origem informado (cioVinculadoId), se for um cio do mesmo animal. */
    private void vincularCio(Animal animal, EventoReproducao dados) {
        Long cioId = dados.getCioVinculadoIdInformado();
        dados.setCioVinculado(null);
        if (cioId == null || !TIPOS_COBERTURA.contains(dados.getTipo())) return;

        EventoReproducao cio = repository.findById(cioId)
                .orElseThrow(() -> new IllegalArgumentException("Cio de origem não encontrado: " + cioId));
        if (!animal.getId().equals(cio.getAnimalId()) || !TIPOS_CIO.contains(cio.getTipo())) {
            throw new RegraNegocioException("O cio de origem precisa ser um cio do mesmo animal.");
        }
        if (dados.getData().isBefore(cio.getData())) {
            throw new RegraNegocioException("A cobertura não pode ser anterior ao cio de origem.");
        }
        dados.setCioVinculado(cio);
    }

    private LocalDate dataDaConcepcao(Long animalId, EventoReproducao evento) {
        if (TIPOS_COBERTURA.contains(evento.getTipo())) {
            return evento.getData();
        }

        boolean confirmaPrenhez = "prenhez_confirmada".equals(evento.getTipo())
                || ("diagnostico_prenhez".equals(evento.getTipo()) && "prenhe".equals(evento.getResultado()));
        if (!confirmaPrenhez) {
            return null;
        }

        return repository.findByAnimal_IdAndTipoInOrderByDataDesc(animalId, TIPOS_COBERTURA)
                .stream()
                .map(EventoReproducao::getData)
                .filter(data -> !data.isAfter(evento.getData()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Categoria segue o evento de parto/secagem/pré-parto mais recente por data — registrar um
     * evento antigo não sobrescreve a situação atual, e excluir um evento recalcula a categoria.
     */
    private void atualizarCategoria(Animal animal) {
        repository.findByAnimal_IdAndTipoInOrderByDataDesc(animal.getId(), TIPOS_CATEGORIA)
                .stream().findFirst()
                .ifPresent(ultimo -> {
                    String categoria = "parto".equals(ultimo.getTipo()) ? "Vaca lactante" : "Vaca seca";
                    if (!categoria.equals(animal.getCategoria())) {
                        animal.setCategoria(categoria);
                        animalRepository.save(animal);
                    }
                });
    }

    public EventoReproducao confirmarCio(Long eventoId) {
        EventoReproducao evento = repository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado: " + eventoId));
        animalService.verificarPropriedade(evento.getAnimal());

        if (!"cio_detectado".equals(evento.getTipo())) {
            throw new RegraNegocioException("Apenas eventos de cio detectado podem ser confirmados");
        }

        evento.setDataConfirmacao(LocalDate.now(clock));
        return repository.save(evento);
    }

    @Transactional
    public void remover(Long id) {
        EventoReproducao evento = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        animalService.verificarPropriedade(evento.getAnimal());
        String tipo = evento.getTipo();
        Long animalId = evento.getAnimalId();
        repository.delete(evento);
        repository.flush();

        if (TIPOS_CATEGORIA.contains(tipo)) {
            animalRepository.findById(animalId).ifPresent(this::atualizarCategoria);
        }
    }
}
