package com.bovix.controller;

import com.bovix.model.EventoSaude;
import com.bovix.service.SaudeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SaudeController {

    private final SaudeService service;

    @GetMapping("/fazendas/{fazendaId}/saude")
    public List<EventoSaude> listarPorFazenda(@PathVariable Long fazendaId) {
        return service.listarPorFazenda(fazendaId);
    }

    @PostMapping("/animais/{animalId}/saude")
    public ResponseEntity<EventoSaude> criar(@PathVariable Long animalId, @Valid @RequestBody EventoSaude dados) {
        return ResponseEntity.ok(service.criar(animalId, dados));
    }

    /** Mesmo registro para todos os animais ativos da fazenda (ex.: vacina anual do rebanho). */
    @PostMapping("/fazendas/{fazendaId}/saude/rebanho")
    public List<EventoSaude> criarParaRebanho(@PathVariable Long fazendaId, @Valid @RequestBody EventoSaude dados) {
        return service.criarParaRebanho(fazendaId, dados);
    }

    @DeleteMapping("/saude/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
