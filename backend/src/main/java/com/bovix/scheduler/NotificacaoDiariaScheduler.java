package com.bovix.scheduler;

import com.bovix.dto.AlertaDoDia;
import com.bovix.model.Configuracao;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.service.EmailService;
import com.bovix.service.PainelDoDiaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Envia o "Quadro do dia" por e-mail no horário escolhido por cada fazenda.
 *
 * Regra de envio: se já passou do horário de hoje e ainda não enviou hoje, envia — mesmo que
 * atrasado (até {@link #ATRASO_MAXIMO}). Antes, o envio só acontecia se a API estivesse ligada
 * exatamente na janela de ±10 min do horário; com a API "dormindo" (plano grátis do Render),
 * o e-mail daquele dia simplesmente não saía.
 *
 * Economia de banco: o job acorda a cada 10 min, mas só consulta o banco quando chega a hora
 * do próximo envio previsto (ou quando alguém altera a configuração). Assim o Neon pode
 * desligar o banco quando ninguém está usando o sistema.
 */
@Component
@RequiredArgsConstructor
public class NotificacaoDiariaScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoDiariaScheduler.class);

    /** Depois disso, o e-mail atrasado de hoje não sai mais (um "bom dia" às 22h não ajuda). */
    static final Duration ATRASO_MAXIMO = Duration.ofHours(12);
    private static final Duration NOVA_TENTATIVA_APOS_FALHA = Duration.ofMinutes(30);
    /** Mesmo sem envio previsto, confere o banco pelo menos a cada 6 h. */
    private static final Duration INTERVALO_MAXIMO = Duration.ofHours(6);
    private static final LocalTime HORARIO_PADRAO = LocalTime.of(7, 0);

    private final ConfiguracaoRepository configuracaoRepository;
    private final PainelDoDiaService painelDoDiaService;
    private final EmailService emailService;
    private final Clock clock; // fuso da fazenda (bovix.fuso-horario), não o UTC do servidor
    private final PlatformTransactionManager transactionManager;

    // EPOCH = conferir na próxima rodada (também vale para logo depois que a API sobe)
    private volatile Instant proximaVerificacao = Instant.EPOCH;

    /** Chamado quando uma fazenda muda horário/e-mail/ativação: confere de novo na próxima rodada. */
    public void agendarNovaVerificacao() {
        proximaVerificacao = Instant.EPOCH;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void executar() {
        Instant agora = Instant.now(clock);
        if (agora.isBefore(proximaVerificacao)) return;
        try {
            // transação: config.getFazenda().getNome() é uma relação lazy
            Instant proxima = new TransactionTemplate(transactionManager).execute(status -> verificarEEnviar());
            proximaVerificacao = proxima != null ? proxima : agora.plus(NOVA_TENTATIVA_APOS_FALHA);
        } catch (Exception e) {
            log.error("Falha na verificação do relatório diário", e);
            proximaVerificacao = agora.plus(NOVA_TENTATIVA_APOS_FALHA);
        }
    }

    /** Envia o que estiver pendente e devolve quando precisa conferir de novo. */
    Instant verificarEEnviar() {
        ZoneId zona = clock.getZone();
        Instant agora = Instant.now(clock);
        LocalDate hoje = LocalDate.now(clock);
        Instant proxima = agora.plus(INTERVALO_MAXIMO);

        List<Configuracao> configs = configuracaoRepository.findByNotificacaoAtivaTrue();
        for (Configuracao config : configs) {
            Instant proximaDesta;
            try {
                if (config.getNotificacaoEmail() == null || config.getNotificacaoEmail().isBlank()) continue;

                LocalTime horario = config.getNotificacaoHorario() != null ? config.getNotificacaoHorario() : HORARIO_PADRAO;
                Instant horarioHoje = hoje.atTime(horario).atZone(zona).toInstant();
                Instant horarioAmanha = hoje.plusDays(1).atTime(horario).atZone(zona).toInstant();

                if (hoje.equals(config.getNotificacaoUltimoEnvio())) {
                    proximaDesta = horarioAmanha;                       // já enviou hoje
                } else if (agora.isBefore(horarioHoje)) {
                    proximaDesta = horarioHoje;                         // ainda não chegou a hora
                } else if (agora.isAfter(horarioHoje.plus(ATRASO_MAXIMO))) {
                    proximaDesta = horarioAmanha;                       // atrasado demais: fica para amanhã
                } else {
                    List<AlertaDoDia> alertas = painelDoDiaService.calcularAlertasDaFazenda(config.getFazendaId());
                    String nomeFazenda = config.getFazenda().getNome();
                    emailService.enviarQuadroDoDia(config.getNotificacaoEmail(), nomeFazenda, alertas);

                    config.setNotificacaoUltimoEnvio(hoje);
                    configuracaoRepository.save(config);
                    log.info("Relatório diário enviado — fazenda {}", config.getFazendaId());
                    proximaDesta = horarioAmanha;
                }
            } catch (Exception e) {
                log.error("Falha ao enviar relatório diário da fazenda {}", config.getFazendaId(), e);
                proximaDesta = agora.plus(NOVA_TENTATIVA_APOS_FALHA);
            }
            if (proximaDesta.isBefore(proxima)) proxima = proximaDesta;
        }
        return proxima;
    }
}
