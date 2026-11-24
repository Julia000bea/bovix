package com.bovix.scheduler;

import com.bovix.model.Configuracao;
import com.bovix.model.Fazenda;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.service.EmailService;
import com.bovix.service.PainelDoDiaService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificacaoDiariaSchedulerTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    private final ConfiguracaoRepository configuracaoRepository = mock(ConfiguracaoRepository.class);
    private final PainelDoDiaService painelDoDiaService = mock(PainelDoDiaService.class);
    private final EmailService emailService = mock(EmailService.class);

    private NotificacaoDiariaScheduler schedulerAs(String horaLocal) {
        Instant agora = HOJE.atTime(LocalTime.parse(horaLocal)).atZone(SP).toInstant();
        return new NotificacaoDiariaScheduler(configuracaoRepository, painelDoDiaService, emailService,
                Clock.fixed(agora, SP), mock(PlatformTransactionManager.class));
    }

    private Configuracao config(String horario, LocalDate ultimoEnvio) {
        Fazenda fazenda = Fazenda.builder().id(1L).nome("Sítio Boa Vista").build();
        Configuracao c = Configuracao.builder().id(1L).fazenda(fazenda).notificacaoAtiva(true)
                .notificacaoEmail("produtor@example.com").notificacaoHorario(LocalTime.parse(horario))
                .notificacaoUltimoEnvio(ultimoEnvio).build();
        when(configuracaoRepository.findByNotificacaoAtivaTrue()).thenReturn(List.of(c));
        return c;
    }

    @Test
    void enviaAtrasadoQuandoAApiAcordaDepoisDoHorario() {
        Configuracao c = config("07:00", HOJE.minusDays(1));

        Instant proxima = schedulerAs("09:40").verificarEEnviar();

        verify(emailService).enviarQuadroDoDia(eq("produtor@example.com"), eq("Sítio Boa Vista"), anyList());
        assertThat(c.getNotificacaoUltimoEnvio()).isEqualTo(HOJE);
        // próxima envio é amanhã 07:00, mas o job confere o banco no máximo a cada 6 h (09:40 + 6 h)
        assertThat(proxima).isEqualTo(HOJE.atTime(15, 40).atZone(SP).toInstant());
    }

    @Test
    void naoEnviaAntesDoHorarioEAgendaParaOHorario() {
        config("07:00", HOJE.minusDays(1));

        Instant proxima = schedulerAs("06:30").verificarEEnviar();

        verifyNoInteractions(emailService);
        assertThat(proxima).isEqualTo(HOJE.atTime(7, 0).atZone(SP).toInstant());
    }

    @Test
    void naoEnviaDuasVezesNoMesmoDia() {
        config("07:00", HOJE);

        schedulerAs("07:10").verificarEEnviar();

        verifyNoInteractions(emailService);
    }

    @Test
    void atrasoDeMaisDe12HorasFicaParaAmanha() {
        config("07:00", HOJE.minusDays(1));

        schedulerAs("19:30").verificarEEnviar();

        verifyNoInteractions(emailService);
    }

    @Test
    void falhaNoEnvioTentaDeNovoEm30Minutos() {
        Configuracao c = config("07:00", HOJE.minusDays(1));
        doThrow(new RuntimeException("SMTP fora do ar"))
                .when(emailService).enviarQuadroDoDia(anyString(), anyString(), any());

        Instant proxima = schedulerAs("07:10").verificarEEnviar();

        assertThat(c.getNotificacaoUltimoEnvio()).isEqualTo(HOJE.minusDays(1));
        assertThat(proxima).isEqualTo(HOJE.atTime(7, 40).atZone(SP).toInstant());
    }
}
