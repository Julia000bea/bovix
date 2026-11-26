package com.bovix.service;

import com.bovix.clima.ClimaDiario;
import com.bovix.dto.LocalizacaoRequest;
import com.bovix.model.Fazenda;
import com.bovix.repository.ClimaRepository;
import com.bovix.repository.FazendaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClimaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final ClimaRepository climaRepository = mock(ClimaRepository.class);
    private final FazendaRepository fazendaRepository = mock(FazendaRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    private final ClimaService service = new ClimaService(climaRepository, fazendaRepository, fazendaService,
            Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    private Fazenda fazenda(String estacao) {
        Fazenda f = Fazenda.builder().id(1L).estacaoInmet(estacao).build();
        when(fazendaService.verificarPropriedade(1L)).thenReturn(f);
        return f;
    }

    @Test
    void serieUsaAEstacaoDaFazenda() {
        fazenda("A001");
        List<ClimaDiario> dados = List.of(new ClimaDiario("A001", HOJE, 25.0, null, null, 70.0, 74.0, null, null, "csv"));
        when(climaRepository.serie("A001", HOJE.minusDays(7), HOJE)).thenReturn(dados);

        assertThat(service.serie(1L, HOJE.minusDays(7), HOJE)).isEqualTo(dados);
    }

    @Test
    void naoAcessaClimaNemLocalizacaoDeOutraFazenda() {
        when(fazendaService.verificarPropriedade(2L)).thenThrow(new AccessDeniedException("não é sua"));

        assertThatThrownBy(() -> service.serie(2L, HOJE, HOJE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.atualizarLocalizacao(2L, new LocalizacaoRequest())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.importar(2L, new MockMultipartFile("arquivo", new byte[]{1})))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(climaRepository, fazendaRepository);
    }

    @Test
    void localizacaoNormalizaAEstacao() {
        Fazenda f = fazenda(null);
        when(fazendaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalizacaoRequest req = new LocalizacaoRequest();
        req.setLatitude(new BigDecimal("-15.7894444"));
        req.setLongitude(new BigDecimal("-47.9258333"));
        req.setEstacaoInmet(" a001 ");

        service.atualizarLocalizacao(1L, req);

        assertThat(f.getEstacaoInmet()).isEqualTo("A001");
        assertThat(f.getLatitude()).isEqualByComparingTo("-15.789444");
    }

    @Test
    void importacaoRecusaArquivoDeOutraEstacao() {
        fazenda("A001");
        var arquivo = new MockMultipartFile("arquivo", "inmet.csv", "text/csv",
                "CODIGO (WMO):;B999\nData;Hora UTC;x\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.importar(1L, arquivo))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("B999");
    }

    @Test
    void importacaoExigeEstacaoConfigurada() {
        fazenda(null);
        assertThatThrownBy(() -> service.importar(1L, new MockMultipartFile("arquivo", new byte[]{1})))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void importaCsvDiario() throws Exception {
        fazenda("A001");
        when(climaRepository.salvarOuSubstituir(any())).thenReturn(false, true);
        var arquivo = new MockMultipartFile("arquivo", "clima.csv", "text/csv", """
                data;temp_media;temp_max;temp_min;umidade_media
                2026-10-01;25;30;20;70
                2026-10-02;26;31;21;65
                """.getBytes(StandardCharsets.UTF_8));

        var resumo = service.importar(1L, arquivo);

        assertThat(resumo.importados()).isEqualTo(1);
        assertThat(resumo.substituidos()).isEqualTo(1);
        assertThat(resumo.primeiroDia()).isEqualTo(LocalDate.of(2026, 10, 1));
    }
}
