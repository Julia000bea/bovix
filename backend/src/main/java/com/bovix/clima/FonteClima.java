package com.bovix.clima;

import java.time.LocalDate;
import java.util.List;

/** De onde vem o clima diário de uma estação: API do INMET ou arquivo CSV histórico. */
public interface FonteClima {

    /** Identificação gravada na coluna clima_diario.fonte ("inmet" ou "csv"). */
    String nome();

    /**
     * Dias completos de clima da estação entre inicio e fim (inclusive). Dias sem dados suficientes
     * não vêm na lista.
     *
     * @throws FonteClimaException se a fonte falhar (fora do ar, timeout, resposta inválida)
     */
    List<ClimaDiario> buscar(String estacao, LocalDate inicio, LocalDate fim) throws FonteClimaException;
}
