package com.bovix.controller;

import com.bovix.model.Ocorrencia;
import com.bovix.service.OcorrenciaService;
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
public class OcorrenciaController {

    private final OcorrenciaService service;

    @GetMapping("/fazendas/{fazendaId}/ocorrencias")
    public List<Ocorrencia> listar(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listar(fazendaId, inicio, fim);
    }

    @PostMapping("/fazendas/{fazendaId}/ocorrencias")
    public Ocorrencia criar(@PathVariable Long fazendaId, @Valid @RequestBody Ocorrencia dados) {
        return service.criar(fazendaId, dados);
    }

    @PutMapping("/ocorrencias/{id}")
    public Ocorrencia atualizar(@PathVariable Long id, @Valid @RequestBody Ocorrencia dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/ocorrencias/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
