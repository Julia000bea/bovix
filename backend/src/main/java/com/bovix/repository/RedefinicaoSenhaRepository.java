package com.bovix.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tokens de "esqueci minha senha" e a data da última troca de senha de cada usuário.
 * Datas em milissegundos (UTC), via JdbcTemplate — ver migração V9.
 */
@Repository
@RequiredArgsConstructor
public class RedefinicaoSenhaRepository {

    private final JdbcTemplate jdbc;

    /** Invalida links anteriores do usuário e grava o novo (só o hash do token). */
    public void criarToken(Long usuarioId, String tokenHash, long expiraEm, long agora) {
        jdbc.update("UPDATE tokens_redefinicao_senha SET usado_em = ? WHERE usuario_id = ? AND usado_em IS NULL",
                agora, usuarioId);
        jdbc.update("INSERT INTO tokens_redefinicao_senha (usuario_id, token_hash, expira_em, criado_em) VALUES (?, ?, ?, ?)",
                usuarioId, tokenHash, expiraEm, agora);
    }

    /** Marca o token como usado e devolve o usuário dono, se o token existir, não expirou e não foi usado. */
    public Optional<Long> consumirToken(String tokenHash, long agora) {
        List<Long> ids = jdbc.queryForList("""
                UPDATE tokens_redefinicao_senha
                   SET usado_em = ?
                 WHERE token_hash = ? AND usado_em IS NULL AND expira_em > ?
                RETURNING usuario_id
                """, Long.class, agora, tokenHash, agora);
        return ids.stream().findFirst();
    }

    public void registrarTrocaDeSenha(Long usuarioId, long quando) {
        jdbc.update("UPDATE usuarios SET senha_alterada_em = ? WHERE id = ?", quando, usuarioId);
    }

    /** Milissegundos da última troca de senha, ou null se nunca trocou. */
    public Long senhaAlteradaEm(Long usuarioId) {
        List<Long> valores = jdbc.queryForList("SELECT senha_alterada_em FROM usuarios WHERE id = ?", Long.class, usuarioId);
        return valores.isEmpty() ? null : valores.get(0);
    }
}
