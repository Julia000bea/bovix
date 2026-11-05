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
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A cada 10 minutos, confere se alguma fazenda com notificação ativa está dentro da janela do
 * horário configurado e ainda não recebeu o e-mail hoje. Se sim, calcula o quadro do dia e envia.
 * @Transactional porque precisamos ler config.getFazenda().getNome() (relação lazy) dentro do loop.
 */
@Component
@RequiredArgsConstructor
public class NotificacaoDiariaScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoDiariaScheduler.class);
    private static final int JANELA_MINUTOS = 10; // deve bater com o cron abaixo

    private final ConfiguracaoRepository configuracaoRepository;
    private final PainelDoDiaService painelDoDiaService;
    private final EmailService emailService;
    private final Clock clock; // fuso da fazenda (bovix.fuso-horario), não o UTC do servidor

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void verificarEEnviar() {
        LocalDate hoje = LocalDate.now(clock);
        LocalTime agora = LocalTime.now(clock);

        List<Configuracao> configs = configuracaoRepository.findByNotificacaoAtivaTrue();
        for (Configuracao config : configs) {
            try {
                if (config.getNotificacaoEmail() == null || config.getNotificacaoEmail().isBlank()) continue;
                if (hoje.equals(config.getNotificacaoUltimoEnvio())) continue; // já enviou hoje

                LocalTime horario = config.getNotificacaoHorario();
                long diferencaMinutos = Math.abs(ChronoUnit.MINUTES.between(horario, agora));
                if (diferencaMinutos > JANELA_MINUTOS) continue; // ainda não chegou a janela de hoje

                List<AlertaDoDia> alertas = painelDoDiaService.calcularAlertasDaFazenda(config.getFazendaId());
                String nomeFazenda = config.getFazenda().getNome();
                emailService.enviarQuadroDoDia(config.getNotificacaoEmail(), nomeFazenda, alertas);

                config.setNotificacaoUltimoEnvio(hoje);
                configuracaoRepository.save(config);
                log.info("Relatório diário enviado — fazenda {}", config.getFazendaId());
            } catch (Exception e) {
                log.error("Falha ao enviar relatório diário da fazenda {}", config.getFazendaId(), e);
            }
        }
    }
}
