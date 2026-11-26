package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.model.Ocorrencia;
import com.bovix.repository.OcorrenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    public static final Set<String> TIPOS = Set.of("alimentacao", "agua", "equipamento", "clima", "sanidade", "manejo", "outro");

    private final OcorrenciaRepository repository;
    private final FazendaService fazendaService;
    private final Clock clock;

    public List<Ocorrencia> listar(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim);
    }

    public Ocorrencia criar(Long fazendaId, Ocorrencia dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        validar(dados);
        dados.setFazenda(fazenda);
        dados.setCriadoEm(null);
        return repository.save(dados);
    }

    public Ocorrencia atualizar(Long id, Ocorrencia dados) {
        Ocorrencia existente = buscar(id);
        validar(dados);
        existente.setData(dados.getData());
        existente.setTipo(dados.getTipo());
        existente.setDescricao(dados.getDescricao().trim());
        return repository.save(existente);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
    }

    /** Busca o registro e confere se a fazenda dele é do usuário logado. */
    private Ocorrencia buscar(Long id) {
        Ocorrencia o = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência não encontrada: " + id));
        fazendaService.verificarPropriedade(o.getFazenda().getId());
        return o;
    }

    private void validar(Ocorrencia dados) {
        if (!TIPOS.contains(dados.getTipo())) {
            throw new RegraNegocioException("Tipo de ocorrência inválido: " + dados.getTipo());
        }
        if (dados.getData().isAfter(LocalDate.now(clock))) {
            throw new RegraNegocioException("A data da ocorrência não pode estar no futuro.");
        }
        dados.setDescricao(dados.getDescricao().trim());
    }
}
