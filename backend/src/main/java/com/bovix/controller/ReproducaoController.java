package com.bovix.controller;

import com.bovix.model.EventoReproducao;
import com.bovix.service.ReproducaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReproducaoController {

    private final ReproducaoService service;

    @GetMapping("/fazendas/{fazendaId}/reproducao")
    public List<EventoReproducao> listarPorFazenda(@PathVariable Long fazendaId) {
        return service.listarPorFazenda(fazendaId);
    }

    @PostMapping("/animais/{animalId}/reproducao")
    public ResponseEntity<EventoReproducao> criar(@PathVariable Long animalId, @Valid @RequestBody EventoReproducao dados) {
        return ResponseEntity.ok(service.criar(animalId, dados));
    }

    @PutMapping("/reproducao/{id}/confirmar-cio")
    public ResponseEntity<EventoReproducao> confirmarCio(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmarCio(id));
    }

    @DeleteMapping("/reproducao/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
