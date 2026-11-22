package com.bovix.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Geração e validação dos tokens JWT usados para autenticar as chamadas à API. */
@Service
public class JwtService {

    // HMAC-SHA256 exige no mínimo 256 bits (32 bytes) de chave.
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Value("${bovix.jwt.secret}")
    private String secret;

    @Value("${bovix.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey chave;

    /** Impede a API de subir sem um segredo próprio — nunca cai num valor padrão conhecido. */
    @PostConstruct
    void validarSegredo() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Variável BOVIX_JWT_SECRET não definida. Gere um valor aleatório (ex: openssl rand -base64 64) e defina-a antes de subir a API.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "BOVIX_JWT_SECRET muito curta: use pelo menos " + TAMANHO_MINIMO_SEGREDO + " bytes aleatórios.");
        }
        chave = Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("uid", usuarioId)
                .issuedAt(agora)
                .expiration(expira)
                .signWith(chave)
                .compact();
    }

    /** Dados de um token válido: id do usuário (claim "uid") e quando foi emitido. */
    public record TokenValido(Long usuarioId, Date emitidoEm) {}

    /** Valida assinatura e expiração; lança exceção se o token for inválido. */
    public TokenValido validar(String token) {
        var claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        Object uid = claims.get("uid");
        Long usuarioId = uid instanceof Number numero ? numero.longValue() : Long.valueOf(String.valueOf(uid));
        return new TokenValido(usuarioId, claims.getIssuedAt());
    }

    /** Retorna o id do usuário (claim "uid") se o token for válido, ou lança exceção. */
    public Long validarEExtrairUsuarioId(String token) {
        return validar(token).usuarioId();
    }
}
