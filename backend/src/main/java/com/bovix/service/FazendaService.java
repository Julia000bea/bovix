package com.bovix.service;

import com.bovix.model.Fazenda;
import com.bovix.repository.FazendaRepository;
import com.bovix.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FazendaService {

    private final FazendaRepository fazendaRepository;

    /**
     * Garante que a fazenda pertence ao usuário autenticado na requisição atual.
     * Toda rota que recebe um fazendaId deve chamar isto antes de ler/escrever dados.
     */
    public Fazenda verificarPropriedade(Long fazendaId) {
        Fazenda fazenda = fazendaRepository.findById(fazendaId)
                .orElseThrow(() -> new IllegalArgumentException("Fazenda não encontrada: " + fazendaId));
        Long usuarioId = SecurityUtils.usuarioAtual().getId();
        if (!fazenda.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("Esta fazenda não pertence ao usuário autenticado.");
        }
        return fazenda;
    }
}
