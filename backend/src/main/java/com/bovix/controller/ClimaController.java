package com.bovix.controller;

import com.bovix.clima.ClimaDiario;
import com.bovix.dto.LocalizacaoRequest;
import com.bovix.model.Fazenda;
import com.bovix.service.ClimaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fazendas/{fazendaId}")
@RequiredArgsConstructor
public class ClimaController {

    private final ClimaService climaService;

    /** Clima diário (temperaturas, umidade e ITU) da estação da fazenda no período. */
    @GetMapping("/clima")
    public List<ClimaDiario> serie(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return climaService.serie(fazendaId, inicio, fim);
    }

    /** CSV histórico de clima (arquivo horário do INMET ou diário simples). */
    @PostMapping("/clima/importar")
    public ClimaService.ResumoImportacao importar(@PathVariable Long fazendaId,
                                                  @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return climaService.importar(fazendaId, arquivo);
    }

    /** Latitude, longitude e estação do INMET da fazenda. */
    @PutMapping("/localizacao")
    public Fazenda atualizarLocalizacao(@PathVariable Long fazendaId, @Valid @RequestBody LocalizacaoRequest dados) {
        return climaService.atualizarLocalizacao(fazendaId, dados);
    }
}
