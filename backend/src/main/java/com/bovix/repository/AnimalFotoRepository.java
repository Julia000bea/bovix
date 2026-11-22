package com.bovix.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Fotos dos animais guardadas no PostgreSQL (tabela animal_fotos, coluna BYTEA).
 * JdbcTemplate em vez de entidade JPA: a foto nunca é carregada junto com o animal
 * e o Hibernate não precisa validar o tipo binário da coluna.
 */
@Repository
@RequiredArgsConstructor
public class AnimalFotoRepository {

    public record Foto(String contentType, byte[] conteudo) {}

    private final JdbcTemplate jdbc;

    /** Grava ou substitui a foto do animal (uma por animal). */
    public void salvar(Long animalId, String chave, String contentType, byte[] conteudo) {
        jdbc.update("""
                INSERT INTO animal_fotos (animal_id, chave, content_type, conteudo, atualizado_em)
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (animal_id) DO UPDATE
                   SET chave = EXCLUDED.chave,
                       content_type = EXCLUDED.content_type,
                       conteudo = EXCLUDED.conteudo,
                       atualizado_em = now()
                """, animalId, chave, contentType, conteudo);
    }

    public Optional<Foto> buscarPorChave(String chave) {
        List<Foto> fotos = jdbc.query(
                "SELECT content_type, conteudo FROM animal_fotos WHERE chave = ?",
                (rs, i) -> new Foto(rs.getString("content_type"), rs.getBytes("conteudo")),
                chave);
        return fotos.stream().findFirst();
    }
}
