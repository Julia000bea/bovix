package com.bovix.clima;

import java.time.LocalDate;

/**
 * Clima de um dia numa estação. Valores sem dado ficam null.
 * horasItuAcima72 só existe quando o dia veio de leituras horárias.
 */
public record ClimaDiario(String estacao, LocalDate data, Double tempMedia, Double tempMax, Double tempMin,
                          Double umidadeMedia, Double ituMedio, Double ituMax, Integer horasItuAcima72, String fonte) {}
