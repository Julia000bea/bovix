package com.bovix.service;

import com.bovix.model.Configuracao;
import com.bovix.model.Fazenda;
import com.bovix.repository.ConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository repository;
    private final FazendaService fazendaService;

    public Configuracao buscarOuCriar(Long fazendaId) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        return repository.findByFazenda_Id(fazendaId).orElseGet(() -> {
            Configuracao nova = Configuracao.builder().fazenda(fazenda).gestacaoDias(283).cioDias(21).build();
            return repository.save(nova);
        });
    }

    public Configuracao atualizar(Long fazendaId, Configuracao dados) {
        Configuracao config = buscarOuCriar(fazendaId);
        if (dados.getGestacaoDias() != null) config.setGestacaoDias(dados.getGestacaoDias());
        if (dados.getCioDias() != null) config.setCioDias(dados.getCioDias());
        if (dados.getNotificacaoAtiva() != null) config.setNotificacaoAtiva(dados.getNotificacaoAtiva());
        if (dados.getNotificacaoEmail() != null) config.setNotificacaoEmail(dados.getNotificacaoEmail());
        if (dados.getNotificacaoHorario() != null) config.setNotificacaoHorario(dados.getNotificacaoHorario());
        return repository.save(config);
    }
}
