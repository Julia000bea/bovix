package com.bovix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroRequest {
    @NotBlank
    private String nome;

    @NotBlank @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotBlank
    private String fazendaNome;

    private String cidade;
    @Size(max = 2)
    private String estado;
}
