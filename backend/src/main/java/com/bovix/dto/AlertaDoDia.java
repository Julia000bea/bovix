package com.bovix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Um item do "Quadro do dia" — calculado só no backend e usado pela tela e pelo e-mail automático. */
@Data
@AllArgsConstructor
public class AlertaDoDia {
    private String id;           // estável por animal/evento, usado como key no frontend
    private Long animalId;
    private String animalBrinco;
    private String animalNome;
    private String tipo;         // parto | preparto | secagem | diagnostico | cio | carencia | saude
    private String urgencia;     // atrasado | urgente | ativo | proximo
    private Long dias;           // dias até a data do alerta (negativo = atrasado); null quando não se aplica
    private String texto;
}
