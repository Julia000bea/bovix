package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.model.Financeiro;
import com.bovix.repository.FinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final FinanceiroRepository repository;
    private final FazendaService fazendaService;

    public List<Financeiro> listarPorPeriodo(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim);
    }

    public Financeiro criar(Long fazendaId, Financeiro dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        dados.setFazenda(fazenda);
        return repository.save(dados);
    }

    public void remover(Long id) {
        Financeiro registro = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        fazendaService.verificarPropriedade(registro.getFazenda().getId());
        repository.deleteById(id);
    }
}
