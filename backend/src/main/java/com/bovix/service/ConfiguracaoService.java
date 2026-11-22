package com.bovix.service;

import com.bovix.dto.ConfiguracaoRequest;
import com.bovix.model.Configuracao;
import com.bovix.model.Fazenda;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.scheduler.NotificacaoDiariaScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository repository;
    private final FazendaService fazendaService;
    private final NotificacaoDiariaScheduler notificacaoScheduler;

    public Configuracao buscarOuCriar(Long fazendaId) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_Id(fazendaId).orElseGet(() -> {
            Configuracao nova = Configuracao.builder().fazenda(fazenda).gestacaoDias(283).cioDias(21).build();
            return repository.save(nova);
        });
    }

    public Configuracao atualizar(Long fazendaId, ConfiguracaoRequest dados) {
        Configuracao config = buscarOuCriar(fazendaId);
        if (dados.getGestacaoDias() != null) config.setGestacaoDias(dados.getGestacaoDias());
        if (dados.getCioDias() != null) config.setCioDias(dados.getCioDias());
        if (dados.getNotificacaoAtiva() != null) config.setNotificacaoAtiva(dados.getNotificacaoAtiva());
        if (dados.getNotificacaoEmail() != null) {
            String email = dados.getNotificacaoEmail().trim();
            config.setNotificacaoEmail(email.isEmpty() ? null : email);
        }
        if (dados.getNotificacaoHorario() != null) config.setNotificacaoHorario(dados.getNotificacaoHorario());

        if (Boolean.TRUE.equals(config.getNotificacaoAtiva())
                && (config.getNotificacaoEmail() == null || config.getNotificacaoEmail().isBlank())) {
            throw new RegraNegocioException("Informe o e-mail de destino para ativar o relatório diário.");
        }
        Configuracao salva = repository.save(config);
        notificacaoScheduler.agendarNovaVerificacao(); // horário/e-mail podem ter mudado
        return salva;
    }
}
