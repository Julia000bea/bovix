package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.model.Financeiro;
import com.bovix.repository.FinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lançamentos financeiros: despesas (padrão) e receitas (venda de leite).
 * Os totais e insights de custo continuam contando só as despesas (filtradas por tipo no frontend).
 */
@Service
@RequiredArgsConstructor
public class FinanceiroService {

    static final Set<String> CATEGORIAS_DESPESA = Set.of("Ração", "Insumos", "Medicamentos", "Mão de obra", "Manutenção", "Outros");
    static final Set<String> CATEGORIAS_RECEITA = Set.of("Venda de leite", "Outros");

    private final FinanceiroRepository repository;
    private final FazendaService fazendaService;

    public List<Financeiro> listarPorPeriodo(Long fazendaId, LocalDate inicio, LocalDate fim) {
        fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, inicio, fim);
    }

    public Financeiro criar(Long fazendaId, Financeiro dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        validar(dados);
        dados.setFazenda(fazenda);
        return repository.save(dados);
    }

    void validar(Financeiro dados) {
        String tipo = dados.getTipo() == null || dados.getTipo().isBlank() ? "despesa" : dados.getTipo();
        dados.setTipo(tipo);
        Set<String> permitidas = switch (tipo) {
            case "despesa" -> CATEGORIAS_DESPESA;
            case "receita" -> CATEGORIAS_RECEITA;
            default -> throw new RegraNegocioException("Tipo de lançamento inválido: use despesa ou receita.");
        };
        if (!permitidas.contains(dados.getCategoria())) {
            throw new RegraNegocioException("Categoria \"" + dados.getCategoria() + "\" não vale para " + tipo + ".");
        }
    }

    public void remover(Long id) {
        Financeiro registro = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
        fazendaService.verificarPropriedade(registro.getFazenda().getId());
        repository.deleteById(id);
    }
}
