package com.bovix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Um item do "Quadro do dia" — mesma lógica usada no frontend, calculada aqui para os e-mails automáticos. */
@Data
@AllArgsConstructor
public class AlertaDoDia {
    private String animalBrinco;
    private String animalNome;
    private String urgencia; // atrasado | urgente | ativo | proximo
    private String texto;
}
