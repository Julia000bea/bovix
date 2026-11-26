package com.bovix.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/** Localização da fazenda e estação do INMET. Null ou vazio = remover. */
@Data
public class LocalizacaoRequest {

    @DecimalMin(value = "-90", message = "deve estar entre -90 e 90")
    @DecimalMax(value = "90", message = "deve estar entre -90 e 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "deve estar entre -180 e 180")
    @DecimalMax(value = "180", message = "deve estar entre -180 e 180")
    private BigDecimal longitude;

    // código das estações automáticas do INMET: uma letra e três números (ex.: A001)
    @Pattern(regexp = "^$|^[A-Za-z]\\d{3}$", message = "use o código da estação do INMET (uma letra e três números, ex.: A001)")
    private String estacaoInmet;
}
