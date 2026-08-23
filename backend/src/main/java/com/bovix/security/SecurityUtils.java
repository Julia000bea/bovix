package com.bovix.security;

import com.bovix.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    /** Usuário autenticado da requisição atual (definido pelo JwtAuthFilter). */
    public static Usuario usuarioAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new AccessDeniedException("Requisição não autenticada.");
        }
        return usuario;
    }
}
