package com.bovix.clima;

import java.time.Instant;

/**
 * Uma leitura horária de estação. temperatura/umidade = valores instantâneos da hora;
 * tempMax/tempMin = máxima e mínima da hora (quando a estação informa). Null = sem dado.
 */
public record LeituraHoraria(Instant instante, Double temperatura, Double umidade, Double tempMax, Double tempMin) {}
