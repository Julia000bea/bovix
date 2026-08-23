package com.bovix.controller;

import com.bovix.model.Fazenda;
import com.bovix.repository.FazendaRepository;
import com.bovix.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fazendas")
@RequiredArgsConstructor
public class FazendaController {

    private final FazendaRepository fazendaRepository;

    /** Lista as fazendas do usuário autenticado (suporte a múltiplas fazendas por conta). */
    @GetMapping("/minhas")
    public List<Fazenda> minhas() {
        return fazendaRepository.findByUsuario_Id(SecurityUtils.usuarioAtual().getId());
    }
}
