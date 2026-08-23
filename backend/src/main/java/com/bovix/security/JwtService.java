package com.bovix.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Geração e validação dos tokens JWT usados para autenticar as chamadas à API. */
@Service
public class JwtService {

    @Value("${bovix.jwt.secret}")
    private String secret;

    @Value("${bovix.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        // HMAC-SHA precisa de uma chave com tamanho mínimo; em produção use um segredo longo e aleatório.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("uid", usuarioId)
                .issuedAt(agora)
                .expiration(expira)
                .signWith(key())
                .compact();
    }

    /** Retorna o id do usuário (claim "uid") se o token for válido, ou lança exceção. */
    public Long validarEExtrairUsuarioId(String token) {
        var claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        Object uid = claims.get("uid");
        if (uid instanceof Number numero) return numero.longValue();
        return Long.valueOf(String.valueOf(uid));
    }
}
