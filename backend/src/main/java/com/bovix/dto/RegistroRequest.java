package com.bovix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroRequest {
    @NotBlank
    private String nome;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String senha;

    @NotBlank
    private String fazendaNome;

    private String cidade;
    private String estado;
}
