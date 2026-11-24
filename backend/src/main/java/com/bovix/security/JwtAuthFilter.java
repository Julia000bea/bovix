package com.bovix.security;

import com.bovix.model.Usuario;
import com.bovix.repository.RedefinicaoSenhaRepository;
import com.bovix.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o header "Authorization: Bearer <token>", valida o JWT e, se válido,
 * autentica a requisição colocando o Usuario no SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final RedefinicaoSenhaRepository redefinicaoSenhaRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtService.TokenValido valido = jwtService.validar(token);
                Usuario usuario = usuarioRepository.findById(valido.usuarioId()).orElse(null);
                if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null
                        && !emitidoAntesDaTrocaDeSenha(valido)) {
                    var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignorado) {
                // token ausente/expirado/inválido: segue sem autenticar — o endpoint protegido devolverá 401/403
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Depois de redefinir a senha, os logins feitos antes (em qualquer aparelho) deixam de valer. */
    private boolean emitidoAntesDaTrocaDeSenha(JwtService.TokenValido token) {
        Long alteradaEm = redefinicaoSenhaRepository.senhaAlteradaEm(token.usuarioId());
        if (alteradaEm == null || token.emitidoEm() == null) return false;
        // o JWT guarda a emissão em segundos: compara na mesma precisão
        return token.emitidoEm().getTime() / 1000 < alteradaEm / 1000;
    }
}
