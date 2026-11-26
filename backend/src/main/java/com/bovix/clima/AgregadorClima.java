package com.bovix.clima;

import com.bovix.calculo.Itu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Agrega leituras horárias em clima diário, no fuso da fazenda (o INMET informa a hora em UTC).
 * O ITU é calculado hora a hora e depois agregado (média, máxima e horas acima de 72).
 * Dias com menos de {@link #MIN_HORAS} horas válidas ficam de fora: a média de um dia incompleto
 * distorce o ITU (faltam as horas mais quentes ou as mais frias) e o dia é buscado de novo depois.
 */
public final class AgregadorClima {

    public static final int MIN_HORAS = 18;

    private AgregadorClima() {}

    public static List<ClimaDiario> agregar(List<LeituraHoraria> leituras, String estacao, String fonte, ZoneId zona) {
        Map<LocalDate, List<LeituraHoraria>> porDia = new TreeMap<>();
        for (LeituraHoraria l : leituras) {
            if (l.temperatura() == null || l.umidade() == null) continue;
            porDia.computeIfAbsent(l.instante().atZone(zona).toLocalDate(), d -> new ArrayList<>()).add(l);
        }

        List<ClimaDiario> dias = new ArrayList<>();
        porDia.forEach((data, horas) -> {
            // a mesma hora repetida no arquivo conta uma vez só
            List<LeituraHoraria> unicas = horas.stream()
                    .collect(java.util.stream.Collectors.toMap(LeituraHoraria::instante, l -> l, (a, b) -> a, TreeMap::new))
                    .values().stream().toList();
            if (unicas.size() < MIN_HORAS) return;

            double somaT = 0, somaUr = 0, somaItu = 0, maxItu = Double.NEGATIVE_INFINITY;
            double maxT = Double.NEGATIVE_INFINITY, minT = Double.POSITIVE_INFINITY;
            int acima = 0;
            for (LeituraHoraria h : unicas) {
                double itu = Itu.calcular(h.temperatura(), h.umidade());
                somaT += h.temperatura();
                somaUr += h.umidade();
                somaItu += itu;
                maxItu = Math.max(maxItu, itu);
                maxT = Math.max(maxT, h.tempMax() != null ? h.tempMax() : h.temperatura());
                minT = Math.min(minT, h.tempMin() != null ? h.tempMin() : h.temperatura());
                if (itu > Itu.LIMIAR_ESTRESSE) acima++;
            }
            int n = unicas.size();
            dias.add(new ClimaDiario(estacao, data, r(somaT / n), r(maxT), r(minT), r(somaUr / n),
                    r(somaItu / n), r(maxItu), Math.min(acima, 24), fonte));
        });
        return dias;
    }

    /**
     * Dia com dados só diários (sem horas): ITU médio pela temperatura e umidade médias, ITU máximo
     * pela temperatura máxima com a umidade média (aproximação: superestima um pouco, porque na hora
     * mais quente a umidade costuma ser menor). Sem horas acima de 72.
     */
    public static ClimaDiario diario(String estacao, LocalDate data, Double tMedia, Double tMax, Double tMin,
                                     Double urMedia, String fonte) {
        Double ituMedio = tMedia != null && urMedia != null ? r(Itu.calcular(tMedia, urMedia)) : null;
        Double ituMax = tMax != null && urMedia != null ? r(Itu.calcular(tMax, urMedia)) : null;
        return new ClimaDiario(estacao, data, r(tMedia), r(tMax), r(tMin), r(urMedia), ituMedio, ituMax, null, fonte);
    }

    static Double r(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return null;
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
