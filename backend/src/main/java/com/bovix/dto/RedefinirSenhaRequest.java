package com.bovix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RedefinirSenhaRequest {
    @NotBlank
    @Size(max = 100)
    private String token;

    @NotBlank
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String novaSenha;
}
