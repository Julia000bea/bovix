package com.bovix.controller;

import com.bovix.model.Ordenha;
import com.bovix.service.OrdenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrdenhaController {

    private final OrdenhaService service;

    @GetMapping("/fazendas/{fazendaId}/ordenhas")
    public List<Ordenha> listar(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listarPorPeriodo(fazendaId, inicio, fim);
    }

    @PostMapping("/fazendas/{fazendaId}/ordenhas")
    public ResponseEntity<Ordenha> criar(
            @PathVariable Long fazendaId,
            @RequestParam(required = false) Long animalId,
            @Valid @RequestBody Ordenha dados) {
        return ResponseEntity.ok(service.criar(fazendaId, animalId, dados));
    }

    @DeleteMapping("/ordenhas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
