package com.bovix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UsuarioResumo usuario;
    private List<FazendaResumo> fazendas;

    @Data
    @AllArgsConstructor
    public static class UsuarioResumo {
        private Long id;
        private String nome;
        private String email;
    }

    @Data
    @AllArgsConstructor
    public static class FazendaResumo {
        private Long id;
        private String nome;
    }
}
