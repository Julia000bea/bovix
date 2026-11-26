package com.bovix.calculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê um CSV de produção diária do rebanho ("data;litros"), sem acessar banco.
 *
 * Aceita:
 * - datas AAAA-MM-DD ou DD/MM/AAAA;
 * - separador ";" ou "," (se a linha tem ";", ele é o separador);
 * - decimal com vírgula ou ponto ("1.234,5" e "1,234.5" também). Com separador "," e decimal com
 *   vírgula sem aspas (ex.: 2026-01-05,1234,5), a terceira parte é tratada como casas decimais;
 * - uma linha de cabeçalho opcional no início.
 */
public final class LeitorCsvProducao {

    public static final int MAX_LINHAS = 5000;
    public static final BigDecimal MAX_LITROS_DIA = new BigDecimal("100000");

    public record LinhaProducao(int linha, LocalDate data, BigDecimal litros) {}

    public record LinhaRejeitada(int linha, String conteudo, String motivo) {}

    public record Resultado(List<LinhaProducao> validas, List<LinhaRejeitada> rejeitadas) {}

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private LeitorCsvProducao() {}

    /** @throws IllegalStateException se o arquivo passar de {@link #MAX_LINHAS} linhas com conteúdo */
    public static Resultado ler(String texto) {
        List<LinhaProducao> validas = new ArrayList<>();
        List<LinhaRejeitada> rejeitadas = new ArrayList<>();
        Map<LocalDate, Integer> linhaDaData = new HashMap<>();

        String[] linhas = texto.replace("﻿", "").split("\r\n|\r|\n", -1);
        boolean primeiraComConteudo = true;
        int comConteudo = 0;
        for (int i = 0; i < linhas.length; i++) {
            String bruta = linhas[i].trim();
            if (bruta.isEmpty()) continue;
            if (++comConteudo > MAX_LINHAS + 1) { // +1 = cabeçalho
                throw new IllegalStateException("O arquivo tem mais de " + MAX_LINHAS + " linhas.");
            }
            int numero = i + 1;
            List<String> campos = separar(bruta);

            if (primeiraComConteudo) {
                primeiraComConteudo = false;
                if (pareceCabecalho(campos)) continue;
            }

            if (campos.size() == 3 && !bruta.contains(";") && campos.get(2).matches("\\d+")) {
                // 2026-01-05,1234,5 → decimal com vírgula sem aspas
                campos = List.of(campos.get(0), campos.get(1) + "," + campos.get(2));
            }
            if (campos.size() != 2) {
                rejeitadas.add(new LinhaRejeitada(numero, bruta, "esperado 2 colunas: data e litros"));
                continue;
            }

            LocalDate data = lerData(campos.get(0));
            if (data == null) {
                rejeitadas.add(new LinhaRejeitada(numero, bruta, "data inválida (use AAAA-MM-DD ou DD/MM/AAAA)"));
                continue;
            }
            BigDecimal litros = lerNumero(campos.get(1));
            if (litros == null) {
                rejeitadas.add(new LinhaRejeitada(numero, bruta, "litros inválido"));
                continue;
            }
            if (litros.signum() < 0 || litros.compareTo(MAX_LITROS_DIA) > 0) {
                rejeitadas.add(new LinhaRejeitada(numero, bruta, "litros fora do intervalo (0 a 100.000)"));
                continue;
            }
            Integer anterior = linhaDaData.putIfAbsent(data, numero);
            if (anterior != null) {
                rejeitadas.add(new LinhaRejeitada(numero, bruta, "data repetida no arquivo (já na linha " + anterior + ")"));
                continue;
            }
            validas.add(new LinhaProducao(numero, data, litros.setScale(2, java.math.RoundingMode.HALF_UP)));
        }
        return new Resultado(validas, rejeitadas);
    }

    static LocalDate lerData(String valor) {
        String v = valor.trim();
        for (DateTimeFormatter f : List.of(ISO, BR)) {
            try {
                return LocalDate.parse(v, f);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }
        return null;
    }

    /** "1234,5", "1234.5", "1.234,5", "1,234.5" → 1234.5. Um só "." ou "," é sempre decimal. */
    static BigDecimal lerNumero(String valor) {
        String v = valor.trim().replace(" ", "");
        if (v.isEmpty()) return null;
        int ultimoPonto = v.lastIndexOf('.');
        int ultimaVirgula = v.lastIndexOf(',');
        if (ultimoPonto >= 0 && ultimaVirgula >= 0) {
            boolean virgulaDecimal = ultimaVirgula > ultimoPonto;
            v = virgulaDecimal ? v.replace(".", "").replace(',', '.') : v.replace(",", "");
        } else if (ultimaVirgula >= 0) {
            if (v.indexOf(',') != ultimaVirgula) return null; // "1,234,5": ambíguo
            v = v.replace(',', '.');
        } else if (ultimoPonto >= 0 && v.indexOf('.') != ultimoPonto) {
            return null;
        }
        if (!v.matches("-?\\d+(\\.\\d+)?")) return null;
        return new BigDecimal(v);
    }

    private static boolean pareceCabecalho(List<String> campos) {
        return !campos.isEmpty() && lerData(campos.get(0)) == null && campos.get(0).matches(".*[A-Za-zÀ-ú].*");
    }

    /** Separa pelos ";" (se houver) ou ",", respeitando aspas duplas. */
    static List<String> separar(String linha) {
        char sep = linha.indexOf(';') >= 0 ? ';' : ',';
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (c == sep && !entreAspas) {
                campos.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString().trim());
        // ignora separador sobrando no fim da linha ("2026-01-01;10;")
        while (campos.size() > 1 && campos.get(campos.size() - 1).isEmpty()) campos.remove(campos.size() - 1);
        return campos;
    }
}
