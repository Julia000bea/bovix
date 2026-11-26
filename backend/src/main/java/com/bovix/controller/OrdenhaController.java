package com.bovix.controller;

import com.bovix.model.Ordenha;
import com.bovix.service.ImportacaoProducaoService;
import com.bovix.service.OrdenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrdenhaController {

    private final OrdenhaService service;
    private final ImportacaoProducaoService importacaoService;

    @GetMapping("/fazendas/{fazendaId}/ordenhas")
    public List<Ordenha> listar(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listarPorPeriodo(fazendaId, inicio, fim);
    }

    /** Devolve a lista de registros criados (mais de um quando diasColeta > 1). */
    @PostMapping("/fazendas/{fazendaId}/ordenhas")
    public List<Ordenha> criar(
            @PathVariable Long fazendaId,
            @RequestParam(required = false) Long animalId,
            @Valid @RequestBody Ordenha dados) {
        return service.criar(fazendaId, animalId, dados);
    }

    /** CSV "data;litros" com a produção diária do rebanho. Devolve o resumo da importação. */
    @PostMapping("/fazendas/{fazendaId}/ordenhas/importar")
    public ImportacaoProducaoService.Resumo importar(@PathVariable Long fazendaId,
                                                     @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return importacaoService.importar(fazendaId, arquivo);
    }

    @DeleteMapping("/ordenhas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
