package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.OrdenhaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportacaoProducaoServiceTest {

    private final OrdenhaRepository repository = mock(OrdenhaRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final ImportacaoProducaoService service = new ImportacaoProducaoService(repository, fazendaService, clock);
    private final Fazenda fazenda = Fazenda.builder().id(1L).build();
    private final List<Ordenha> salvas = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(fazendaService.verificarPropriedade(1L)).thenReturn(fazenda);
        when(repository.saveAll(anyList())).thenAnswer(i -> {
            salvas.addAll((List<Ordenha>) i.getArgument(0));
            return i.getArgument(0);
        });
    }

    private MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "producao.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importaSubstituiERejeitaComMotivo() throws Exception {
        Ordenha totalAntigo = Ordenha.builder().id(1L).fazenda(fazenda).data(LocalDate.of(2026, 10, 2)).litros(BigDecimal.TEN).build();
        Ordenha porAnimal = Ordenha.builder().id(2L).fazenda(fazenda).animal(Animal.builder().id(9L).build())
                .data(LocalDate.of(2026, 10, 3)).litros(BigDecimal.ONE).build();
        when(repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(eq(1L), any(), any()))
                .thenReturn(List.of(totalAntigo, porAnimal));

        var resumo = service.importar(1L, csv("""
                data;litros
                2026-10-01;500,5
                02/10/2026;510
                2026-10-03;520
                2026-10-29;530
                xx;1
                """));

        assertThat(resumo.importadas()).isEqualTo(1);
        assertThat(resumo.substituidas()).isEqualTo(1);
        assertThat(resumo.rejeitadas()).extracting(r -> r.linha()).containsExactly(4, 5, 6);
        assertThat(resumo.rejeitadas()).extracting(r -> r.motivo())
                .anySatisfy(m -> assertThat(m).contains("por animal"))
                .anySatisfy(m -> assertThat(m).contains("futuro"));
        verify(repository).deleteAll(List.of(totalAntigo));
        assertThat(salvas).extracting(Ordenha::getLitros).containsExactly(new BigDecimal("500.50"), new BigDecimal("510.00"));
        assertThat(salvas).allSatisfy(o -> {
            assertThat(o.getAnimal()).isNull();
            assertThat(o.getEstimado()).isFalse();
        });
    }

    @Test
    void recusaArquivoGrande() {
        byte[] grande = new byte[(int) ImportacaoProducaoService.TAMANHO_MAXIMO + 1];
        var arquivo = new MockMultipartFile("arquivo", "x.csv", "text/csv", grande);
        assertThatThrownBy(() -> service.importar(1L, arquivo)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoImportaParaFazendaDeOutroUsuario() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));
        assertThatThrownBy(() -> service.importar(2L, csv("2026-10-01;1"))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void leArquivoDoExcelEmWindows1252() {
        byte[] latin = "Produção;Litros\n".getBytes(java.nio.charset.Charset.forName("windows-1252"));
        assertThat(ImportacaoProducaoService.decodificar(latin)).startsWith("Produção");
    }
}
