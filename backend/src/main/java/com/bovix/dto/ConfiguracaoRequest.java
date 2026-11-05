package com.bovix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

/**
 * Atualização das configurações da fazenda. Campo ausente (null) = não alterar.
 * Usa um DTO (e não a entidade) porque os @Builder.Default da entidade preencheriam
 * os campos omitidos com o padrão, sobrescrevendo o que já estava salvo.
 */
@Data
public class ConfiguracaoRequest {

    @Min(value = 200, message = "deve estar entre 200 e 330 dias")
    @Max(value = 330, message = "deve estar entre 200 e 330 dias")
    private Integer gestacaoDias;

    @Min(value = 10, message = "deve estar entre 10 e 40 dias")
    @Max(value = 40, message = "deve estar entre 10 e 40 dias")
    private Integer cioDias;

    private Boolean notificacaoAtiva;

    // String vazia = remover o e-mail cadastrado
    @Email(message = "e-mail inválido")
    @Size(max = 180)
    private String notificacaoEmail;

    private LocalTime notificacaoHorario;
}
