package com.bovix.repository;

import com.bovix.clima.ClimaDiario;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tabela clima_diario (JdbcTemplate: gravação idempotente com ON CONFLICT). */
@Repository
@RequiredArgsConstructor
public class ClimaRepository {

    private final JdbcTemplate jdbc;

    private static final String COLUNAS =
            "estacao, data, temp_media, temp_max, temp_min, umidade_media, itu_medio, itu_max, horas_itu_acima_72, fonte";

    private static final RowMapper<ClimaDiario> MAPPER = (rs, i) -> new ClimaDiario(
            rs.getString("estacao"), rs.getDate("data").toLocalDate(),
            dbl(rs, "temp_media"), dbl(rs, "temp_max"), dbl(rs, "temp_min"), dbl(rs, "umidade_media"),
            dbl(rs, "itu_medio"), dbl(rs, "itu_max"),
            (Integer) rs.getObject("horas_itu_acima_72"), rs.getString("fonte"));

    public List<ClimaDiario> serie(String estacao, LocalDate inicio, LocalDate fim) {
        return jdbc.query("SELECT " + COLUNAS + " FROM clima_diario WHERE estacao = ? AND data BETWEEN ? AND ? ORDER BY data",
                MAPPER, estacao, inicio, fim);
    }

    public Set<LocalDate> datasExistentes(String estacao, LocalDate inicio, LocalDate fim) {
        return new HashSet<>(jdbc.query("SELECT data FROM clima_diario WHERE estacao = ? AND data BETWEEN ? AND ?",
                (rs, i) -> rs.getDate("data").toLocalDate(), estacao, inicio, fim));
    }

    /** Estações configuradas em alguma fazenda (as que a tarefa diária precisa manter em dia). */
    public List<String> estacoesEmUso() {
        return jdbc.queryForList("SELECT DISTINCT estacao_inmet FROM fazendas WHERE estacao_inmet IS NOT NULL", String.class);
    }

    /** Grava o dia se ainda não existir. Devolve true se gravou. Rodar duas vezes não duplica. */
    public boolean inserirSeNaoExiste(ClimaDiario d) {
        return jdbc.update("INSERT INTO clima_diario (" + COLUNAS + ") VALUES (?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (estacao, data) DO NOTHING", valores(d)) > 0;
    }

    /** Grava o dia, substituindo o que já existir (importação de CSV). Devolve true se substituiu. */
    public boolean salvarOuSubstituir(ClimaDiario d) {
        Boolean substituiu = jdbc.queryForObject("INSERT INTO clima_diario (" + COLUNAS + ") VALUES (?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (estacao, data) DO UPDATE SET temp_media = EXCLUDED.temp_media, temp_max = EXCLUDED.temp_max, "
                + "temp_min = EXCLUDED.temp_min, umidade_media = EXCLUDED.umidade_media, itu_medio = EXCLUDED.itu_medio, "
                + "itu_max = EXCLUDED.itu_max, horas_itu_acima_72 = EXCLUDED.horas_itu_acima_72, fonte = EXCLUDED.fonte "
                + "RETURNING (xmax <> 0)", Boolean.class, valores(d));
        return Boolean.TRUE.equals(substituiu);
    }

    private static Object[] valores(ClimaDiario d) {
        return new Object[]{d.estacao(), d.data(), d.tempMedia(), d.tempMax(), d.tempMin(), d.umidadeMedia(),
                d.ituMedio(), d.ituMax(), d.horasItuAcima72(), d.fonte()};
    }

    private static Double dbl(ResultSet rs, String coluna) throws SQLException {
        BigDecimal v = rs.getBigDecimal(coluna);
        return v == null ? null : v.doubleValue();
    }
}
