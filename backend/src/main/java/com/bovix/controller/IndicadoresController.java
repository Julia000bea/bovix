package com.bovix.controller;

import com.bovix.service.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fazendas/{fazendaId}/indicadores")
@RequiredArgsConstructor
public class IndicadoresController {

    private final IndicadoresService indicadoresService;

    /** Série diária: litros, vacas em lactação, DEL médio e litros por vaca em lactação. */
    @GetMapping("/serie")
    public List<IndicadoresService.PontoSerie> serie(
            @PathVariable Long fazendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return indicadoresService.serie(fazendaId, inicio, fim);
    }
}
