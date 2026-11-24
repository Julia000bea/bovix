package com.bovix.service;

import com.bovix.dto.AlertaDoDia;
import com.bovix.model.Animal;
import com.bovix.model.EventoReproducao;
import com.bovix.model.EventoSaude;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.repository.EventoSaudeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PainelDoDiaServiceTest {

    private static final Long FAZENDA = 1L;
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final EventoReproducaoRepository reproducaoRepository = mock(EventoReproducaoRepository.class);
    private final EventoSaudeRepository saudeRepository = mock(EventoSaudeRepository.class);
    private final ConfiguracaoRepository configuracaoRepository = mock(ConfiguracaoRepository.class);
    private final FazendaService fazendaService = mock(FazendaService.class);
    // 12:00 UTC = 09:00 em São Paulo, mesmo dia
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private PainelDoDiaService service;
    private Animal vaca;
    private final List<EventoReproducao> repro = new ArrayList<>();
    private final List<EventoSaude> saude = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        service = new PainelDoDiaService(animalRepository, reproducaoRepository, saudeRepository,
                configuracaoRepository, fazendaService, clock);
        vaca = Animal.builder().id(10L).brinco("101").nome("Mimosa").status("Ativa").categoria("Vaca lactante").build();
        when(animalRepository.findByFazenda_IdOrderByBrincoAsc(FAZENDA)).thenAnswer(i -> List.of(vaca));
        when(reproducaoRepository.findByAnimal_Fazenda_IdOrderByDataDesc(FAZENDA)).thenReturn(repro);
        when(saudeRepository.findByAnimal_Fazenda_IdOrderByDataDesc(FAZENDA)).thenReturn(saude);
        when(configuracaoRepository.findByFazenda_Id(anyLong())).thenReturn(Optional.empty()); // 283 / 21
    }

    private EventoReproducao repro(String tipo, int diasAtras) {
        return repro(tipo, diasAtras, null);
    }

    private EventoReproducao repro(String tipo, int diasAtras, String resultado) {
        EventoReproducao e = EventoReproducao.builder().id(proximoId++).animal(vaca).tipo(tipo)
                .data(HOJE.minusDays(diasAtras)).resultado(resultado).build();
        repro.add(e);
        return e;
    }

    private void saude(String produto, int diasAtras, Integer carencia, Integer proxima) {
        saude.add(EventoSaude.builder().id(proximoId++).animal(vaca).tipo("vacina").produto(produto)
                .data(HOJE.minusDays(diasAtras)).carenciaLeiteDias(carencia).proximaAplicacaoDias(proxima).build());
    }

    private List<String> tipos(List<AlertaDoDia> alertas) {
        return alertas.stream().map(AlertaDoDia::getTipo).toList();
    }

    @Test
    void cioDetectadoGeraJanelaDoProximoCio() {
        repro("cio_detectado", 20);

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("cio");
            assertThat(a.getDias()).isEqualTo(1L);
            assertThat(a.getUrgencia()).isEqualTo("proximo");
        });
    }

    @Test
    void partoPrevistoContinuaDepoisDaPrenhezConfirmada() {
        repro("inseminacao", 280);
        repro("diagnostico_prenhez", 240, "prenhe");
        repro("prenhez_confirmada", 200);
        repro("secagem", 60);
        repro("pre_parto", 20);

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(tipos(alertas)).containsExactly("parto");
        assertThat(alertas.get(0).getDias()).isEqualTo(3L);
        assertThat(alertas.get(0).getUrgencia()).isEqualTo("urgente");
    }

    @Test
    void montaNaturalTambemPreveParto() {
        repro("reproducao", 275);
        repro("diagnostico_prenhez", 230, "prenhe");
        repro("secagem", 60);
        repro("pre_parto", 20);

        assertThat(tipos(service.calcularAlertasDaFazenda(FAZENDA))).containsExactly("parto");
    }

    @Test
    void diagnosticoVaziaEncerraGestacaoEPreveRetornoDeCio() {
        repro("inseminacao", 50);
        repro("diagnostico_prenhez", 21, "vazia");

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("cio");
            assertThat(a.getId()).startsWith("retorno-");
            assertThat(a.getDias()).isZero();
        });
    }

    @Test
    void coberturaSemDiagnosticoCobraDiagnosticoPendente() {
        repro("inseminacao", 40);

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("diagnostico");
            assertThat(a.getUrgencia()).isEqualTo("proximo");
        });
    }

    @Test
    void coberturaRecenteAlertaRetornoDeCio() {
        repro("cio_confirmado", 22);
        repro("inseminacao", 21);

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("cio");
            assertThat(a.getId()).startsWith("retorno-");
        });
    }

    @Test
    void novoCioDepoisDaCoberturaSubstituiAGestacao() {
        repro("inseminacao", 41);
        repro("cio_detectado", 20);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).singleElement()
                .satisfies(a -> assertThat(a.getId()).startsWith("cio-"));
    }

    @Test
    void prenhezConfirmadaAvisaSecagemEPreParto() {
        // parto previsto em 283 - 218 = 65 dias → secagem em 5 dias; pré-parto ainda longe
        repro("inseminacao", 218);
        repro("prenhez_confirmada", 180);

        List<AlertaDoDia> alertas = service.calcularAlertasDaFazenda(FAZENDA);

        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("secagem");
            assertThat(a.getDias()).isEqualTo(5L);
        });
    }

    @Test
    void secagemRegistradaEncerraAlertaDeSecagem() {
        repro("inseminacao", 230);
        repro("prenhez_confirmada", 180);
        repro("secagem", 2);

        assertThat(tipos(service.calcularAlertasDaFazenda(FAZENDA))).doesNotContain("secagem");
    }

    @Test
    void partoReiniciaOCiclo() {
        repro("inseminacao", 290);
        repro("parto", 5);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).isEmpty();
    }

    @Test
    void reaplicacaoConsideraSoADoseMaisRecente() {
        saude("Aftosa", 400, null, 365); // dose antiga: estaria "atrasada"
        saude("aftosa ", 30, null, 365); // reaplicada — mesmo produto

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).isEmpty();
    }

    @Test
    void reaplicacaoAtrasadaEhAlertada() {
        saude("Brucelose", 40, null, 30);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("saude");
            assertThat(a.getUrgencia()).isEqualTo("atrasado");
        });
    }

    @Test
    void carenciaAtivaAteOUltimoDia() {
        saude("Antibiótico", 3, 3, null);
        saude("Outro", 4, 3, null);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).singleElement()
                .satisfies(a -> assertThat(a.getTipo()).isEqualTo("carencia"));
    }

    @Test
    void carenciaDeTratamentoDeVariosDiasContaDaUltimaAplicacao() {
        // 1ª dose há 5 dias, 3 dias de tratamento (última dose há 3 dias), carência de 4 dias:
        // contando da 1ª dose já teria acabado ontem; contando da última, vai até daqui a 1 dia.
        saude.add(EventoSaude.builder().id(proximoId++).animal(vaca).tipo("medicamento").produto("Antibiótico")
                .data(HOJE.minusDays(5)).duracaoTratamentoDias(3).carenciaLeiteDias(4).build());

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).singleElement().satisfies(a -> {
            assertThat(a.getTipo()).isEqualTo("carencia");
            assertThat(a.getDias()).isEqualTo(1L);
            assertThat(a.getTexto()).contains("29/10/2026").contains("tratamento até 25/10/2026");
        });
    }

    @Test
    void carenciaZeradaNaoGeraAlerta() {
        saude("Vermífugo", 0, 0, null);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).isEmpty();
    }

    @Test
    void animalInativoNaoGeraAlertas() {
        vaca.setStatus("Vendida");
        repro("cio_detectado", 20);

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).isEmpty();
    }

    @Test
    void calculoDoAgendadorNaoDependeDeUsuarioAutenticado() {
        repro("cio_detectado", 20);

        service.calcularAlertasDaFazenda(FAZENDA);

        verifyNoInteractions(fazendaService);
    }

    @Test
    void calculoDaTelaVerificaPropriedadeDaFazenda() {
        service.calcularAlertas(FAZENDA);

        verify(fazendaService).verificarPropriedade(FAZENDA);
    }

    @Test
    void ordenaPorUrgencia() {
        saude("Brucelose", 40, null, 30); // atrasado
        repro("cio_detectado", 21);       // urgente (hoje)

        assertThat(service.calcularAlertasDaFazenda(FAZENDA)).extracting(AlertaDoDia::getUrgencia)
                .containsExactly("atrasado", "urgente");
    }
}
