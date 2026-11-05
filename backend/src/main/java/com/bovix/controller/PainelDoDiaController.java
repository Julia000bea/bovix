package com.bovix.controller;

import com.bovix.dto.AlertaDoDia;
import com.bovix.service.PainelDoDiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PainelDoDiaController {

    private final PainelDoDiaService service;

    /** Quadro do dia — fonte única dos alertas (a tela e o e-mail usam o mesmo cálculo). */
    @GetMapping("/fazendas/{fazendaId}/alertas")
    public List<AlertaDoDia> alertas(@PathVariable Long fazendaId) {
        return service.calcularAlertas(fazendaId);
    }
}
