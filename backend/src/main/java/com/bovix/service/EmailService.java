package com.bovix.service;

import com.bovix.dto.AlertaDoDia;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${bovix.notificacao.remetente}")
    private String remetente;

    public void enviarQuadroDoDia(String destino, String nomeFazenda, List<AlertaDoDia> alertas) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destino);
        mensagem.setSubject("Bovix — Quadro do dia (" + nomeFazenda + ") — "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        mensagem.setText(montarCorpo(nomeFazenda, alertas));
        mailSender.send(mensagem);
    }

    private String montarCorpo(String nomeFazenda, List<AlertaDoDia> alertas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bom dia! Aqui está o quadro do dia da ").append(nomeFazenda).append(":\n\n");

        if (alertas.isEmpty()) {
            sb.append("Nenhum alerta de cio, parto, carência ou vacina para os próximos dias.\n");
        } else {
            for (AlertaDoDia a : alertas) {
                sb.append("- [").append(rotuloUrgencia(a.getUrgencia())).append("] #").append(a.getAnimalBrinco());
                if (a.getAnimalNome() != null && !a.getAnimalNome().isBlank()) {
                    sb.append(" (").append(a.getAnimalNome()).append(")");
                }
                sb.append(" - ").append(a.getTexto()).append("\n");
            }
        }

        sb.append("\n--\nEnviado automaticamente pelo Bovix. Para ajustar o horário ou desligar esse ")
          .append("e-mail, acesse Configurações dentro do sistema.");
        return sb.toString();
    }

    private String rotuloUrgencia(String urgencia) {
        return switch (urgencia) {
            case "atrasado" -> "ATRASADO";
            case "urgente" -> "URGENTE";
            case "ativo" -> "EM ANDAMENTO";
            default -> "PRÓXIMO";
        };
    }
}
