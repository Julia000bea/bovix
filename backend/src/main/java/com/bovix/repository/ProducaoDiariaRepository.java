package com.bovix.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Total de leite por dia, somado no banco (JdbcTemplate: só agregação, sem carregar entidades).
 *
 * Dias antigos que ainda misturam registros por animal e do rebanho (lançados antes do bloqueio)
 * somam só os registros por animal, e vêm com misturado = true.
 */
@Repository
@RequiredArgsConstructor
public class ProducaoDiariaRepository {

    public record ProducaoDia(LocalDate data, BigDecimal litros, boolean estimado, boolean misturado) {}

    private final JdbcTemplate jdbc;

    public List<ProducaoDia> totaisPorDia(Long fazendaId, LocalDate inicio, LocalDate fim) {
        return jdbc.query("""
                SELECT data,
                       SUM(litros) FILTER (WHERE animal_id IS NOT NULL) AS por_animal,
                       SUM(litros) FILTER (WHERE animal_id IS NULL)     AS rebanho,
                       bool_or(estimado)                               AS estimado
                  FROM ordenhas
                 WHERE fazenda_id = ? AND data BETWEEN ? AND ?
                 GROUP BY data
                 ORDER BY data
                """,
                (rs, i) -> {
                    BigDecimal porAnimal = rs.getBigDecimal("por_animal");
                    BigDecimal rebanho = rs.getBigDecimal("rebanho");
                    boolean misturado = porAnimal != null && rebanho != null;
                    return new ProducaoDia(rs.getDate("data").toLocalDate(),
                            porAnimal != null ? porAnimal : rebanho,
                            rs.getBoolean("estimado"), misturado);
                },
                fazendaId, inicio, fim);
    }
}
