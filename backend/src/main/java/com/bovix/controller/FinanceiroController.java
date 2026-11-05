package com.bovix.controller;

import com.bovix.model.Financeiro;
import com.bovix.service.FinanceiroService;
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
public class FinanceiroController {

    private final FinanceiroService service;

    @GetMapping("/fazendas/{fazendaId}/financeiro")
    public List<Financeiro> listar(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listarPorPeriodo(fazendaId, inicio, fim);
    }

    @PostMapping("/fazendas/{fazendaId}/financeiro")
    public ResponseEntity<Financeiro> criar(@PathVariable Long fazendaId, @Valid @RequestBody Financeiro dados) {
        return ResponseEntity.ok(service.criar(fazendaId, dados));
    }

    @DeleteMapping("/financeiro/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
