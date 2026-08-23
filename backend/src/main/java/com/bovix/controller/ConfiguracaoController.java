package com.bovix.controller;

import com.bovix.dto.AlertaDoDia;
import com.bovix.model.Configuracao;
import com.bovix.model.Fazenda;
import com.bovix.service.ConfiguracaoService;
import com.bovix.service.EmailService;
import com.bovix.service.FazendaService;
import com.bovix.service.PainelDoDiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fazendas/{fazendaId}/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfiguracaoService service;
    private final FazendaService fazendaService;
    private final PainelDoDiaService painelDoDiaService;
    private final EmailService emailService;

    @GetMapping
    public Configuracao buscar(@PathVariable Long fazendaId) {
        return service.buscarOuCriar(fazendaId);
    }

    @PutMapping
    public Configuracao atualizar(@PathVariable Long fazendaId, @RequestBody Configuracao dados) {
        return service.atualizar(fazendaId, dados);
    }

    /** Envia o quadro do dia agora mesmo, pra testar se o e-mail configurado está funcionando. */
    @PostMapping("/notificacao/testar")
    public ResponseEntity<String> testarNotificacao(@PathVariable Long fazendaId) {
        Configuracao config = service.buscarOuCriar(fazendaId);
        if (config.getNotificacaoEmail() == null || config.getNotificacaoEmail().isBlank()) {
            throw new IllegalArgumentException("Cadastre um e-mail de notificação antes de testar.");
        }
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        List<AlertaDoDia> alertas = painelDoDiaService.calcularAlertas(fazendaId);
        emailService.enviarQuadroDoDia(config.getNotificacaoEmail(), fazenda.getNome(), alertas);
        return ResponseEntity.ok("Enviado para " + config.getNotificacaoEmail());
    }
}
