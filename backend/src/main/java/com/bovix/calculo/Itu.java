package com.bovix.calculo;

/**
 * Índice de Temperatura e Umidade (ITU / THI):
 * ITU = (1,8·T + 32) − (0,55 − 0,0055·UR)·(1,8·T − 26)
 * T em °C e UR em % (0 a 100). Acima de 72, vacas leiteiras começam a sofrer estresse térmico.
 */
public final class Itu {

    public static final double LIMIAR_ESTRESSE = 72.0;

    private Itu() {}

    public static double calcular(double temperaturaC, double umidadeRelativa) {
        return (1.8 * temperaturaC + 32) - (0.55 - 0.0055 * umidadeRelativa) * (1.8 * temperaturaC - 26);
    }
}
