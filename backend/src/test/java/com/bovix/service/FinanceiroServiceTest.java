package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.model.Financeiro;
import com.bovix.repository.FinanceiroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinanceiroServiceTest {

    private final FinanceiroRepository repository = mock(FinanceiroRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final FinanceiroService service = new FinanceiroService(repository, fazendaService);

    private Financeiro lancamento(String tipo, String categoria) {
        Financeiro f = Financeiro.builder().data(LocalDate.of(2026, 10, 1)).categoria(categoria).valor(BigDecimal.TEN).build();
        f.setTipo(tipo);
        return f;
    }

    @Test
    void semTipoViraDespesa() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(Fazenda.builder().id(1L).build());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.criar(1L, lancamento(null, "Ração")).getTipo()).isEqualTo("despesa");
    }

    @Test
    void vendaDeLeiteSoComoReceita() {
        service.validar(lancamento("receita", "Venda de leite"));
        assertThatThrownBy(() -> service.validar(lancamento("despesa", "Venda de leite"))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.validar(lancamento("receita", "Ração"))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.validar(lancamento("doacao", "Outros"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoLancaEmFazendaDeOutroUsuario() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));
        assertThatThrownBy(() -> service.criar(2L, lancamento("receita", "Venda de leite"))).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }
}
