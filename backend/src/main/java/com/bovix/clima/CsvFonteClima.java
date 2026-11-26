package com.bovix.clima;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Clima a partir de um CSV histórico. Dois formatos:
 *
 * 1. Arquivo horário do INMET ("Dados históricos" do portal, um arquivo por estação e ano):
 *    linhas de cabeçalho com REGIAO, UF, ESTACAO, CODIGO (WMO)... e depois as colunas
 *    "Data;Hora UTC;...;TEMPERATURA DO AR - BULBO SECO, HORARIA (°C);...;UMIDADE RELATIVA DO AR, HORARIA (%)".
 *    Separador ";", decimal com vírgula, -9999 = sem dado. A hora é UTC.
 *
 * 2. CSV diário simples, com cabeçalho: data;temp_media;temp_max;temp_min;umidade_media
 *    (temp_max e temp_min opcionais). Sem horas, o dia não tem "horas com ITU acima de 72".
 */
public class CsvFonteClima implements FonteClima {

    public static final int MAX_LINHAS = 100_000;

    private static final List<DateTimeFormatter> DATAS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd"), DateTimeFormatter.ofPattern("uuuu/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu"));

    private final List<String> linhas;
    private final ZoneId zona;

    public CsvFonteClima(String conteudo, ZoneId zona) {
        this.linhas = List.of(conteudo.replace("﻿", "").split("\r\n|\r|\n"));
        this.zona = zona;
        if (linhas.size() > MAX_LINHAS) {
            throw new IllegalArgumentException("O arquivo tem mais de " + MAX_LINHAS + " linhas.");
        }
    }

    @Override
    public String nome() {
        return "csv";
    }

    /** Código da estação informado no cabeçalho do arquivo do INMET ("CODIGO (WMO):;A001"), ou null. */
    public String estacaoDoArquivo() {
        for (String linha : linhas.subList(0, Math.min(15, linhas.size()))) {
            String n = normalizar(linha);
            if (n.startsWith("CODIGO")) {
                String[] partes = linha.split("[;:]");
                for (int i = partes.length - 1; i >= 0; i--) {
                    String p = partes[i].trim().toUpperCase(Locale.ROOT);
                    if (p.matches("[A-Z]\\d{3}")) return p;
                }
            }
        }
        return null;
    }

    @Override
    public List<ClimaDiario> buscar(String estacao, LocalDate inicio, LocalDate fim) throws FonteClimaException {
        int cab = acharCabecalho();
        if (cab < 0) {
            throw new FonteClimaException("Formato não reconhecido. Use o arquivo horário do INMET (Dados históricos) "
                    + "ou um CSV com as colunas data;temp_media;temp_max;temp_min;umidade_media.");
        }
        List<String> colunas = separar(linhas.get(cab));
        List<ClimaDiario> dias = colunas.stream().anyMatch(c -> normalizar(c).contains("BULBO SECO"))
                ? lerHorario(cab, colunas, estacao)
                : lerDiario(cab, colunas, estacao);
        return dias.stream()
                .filter(d -> (inicio == null || !d.data().isBefore(inicio)) && (fim == null || !d.data().isAfter(fim)))
                .toList();
    }

    private int acharCabecalho() {
        for (int i = 0; i < Math.min(30, linhas.size()); i++) {
            List<String> cols = separar(linhas.get(i)).stream().map(CsvFonteClima::normalizar).toList();
            boolean inmet = cols.stream().anyMatch(c -> c.contains("BULBO SECO"))
                    && cols.stream().anyMatch(c -> c.contains("UMIDADE RELATIVA DO AR, HORARIA"));
            boolean diario = cols.contains("DATA") && cols.contains("TEMP_MEDIA") && cols.contains("UMIDADE_MEDIA");
            if (inmet || diario) return i;
        }
        return -1;
    }

    private List<ClimaDiario> lerHorario(int cab, List<String> colunas, String estacao) throws FonteClimaException {
        int cData = indice(colunas, c -> c.startsWith("DATA"));
        int cHora = indice(colunas, c -> c.startsWith("HORA"));
        int cTemp = indice(colunas, c -> c.contains("BULBO SECO"));
        int cUr = indice(colunas, c -> c.contains("UMIDADE RELATIVA DO AR, HORARIA"));
        int cMax = indice(colunas, c -> c.contains("TEMPERATURA MAXIMA NA HORA"));
        int cMin = indice(colunas, c -> c.contains("TEMPERATURA MINIMA NA HORA"));
        if (cData < 0 || cHora < 0) throw new FonteClimaException("Arquivo do INMET sem as colunas de data e hora.");

        List<LeituraHoraria> leituras = new ArrayList<>();
        for (int i = cab + 1; i < linhas.size(); i++) {
            List<String> v = separar(linhas.get(i));
            if (v.size() <= Math.max(cTemp, cUr)) continue;
            LocalDate data = data(v.get(cData));
            Integer hora = hora(v.get(cHora));
            if (data == null || hora == null) continue;
            leituras.add(new LeituraHoraria(data.atTime(LocalTime.of(hora, 0)).toInstant(ZoneOffset.UTC),
                    numero(v.get(cTemp)), numero(v.get(cUr)),
                    cMax >= 0 && cMax < v.size() ? numero(v.get(cMax)) : null,
                    cMin >= 0 && cMin < v.size() ? numero(v.get(cMin)) : null));
        }
        return AgregadorClima.agregar(leituras, estacao, nome(), zona);
    }

    private List<ClimaDiario> lerDiario(int cab, List<String> colunas, String estacao) {
        List<String> n = colunas.stream().map(CsvFonteClima::normalizar).toList();
        int cData = n.indexOf("DATA");
        int cMedia = n.indexOf("TEMP_MEDIA");
        int cMax = n.indexOf("TEMP_MAX");
        int cMin = n.indexOf("TEMP_MIN");
        int cUr = n.indexOf("UMIDADE_MEDIA");
        List<ClimaDiario> dias = new ArrayList<>();
        java.util.Set<LocalDate> vistos = new java.util.HashSet<>();
        for (int i = cab + 1; i < linhas.size(); i++) {
            List<String> v = separar(linhas.get(i));
            if (v.size() <= Math.max(cData, Math.max(cMedia, cUr))) continue;
            LocalDate data = data(v.get(cData));
            Double media = numero(v.get(cMedia));
            Double ur = numero(v.get(cUr));
            if (data == null || media == null || ur == null || !vistos.add(data)) continue;
            dias.add(AgregadorClima.diario(estacao, data, media,
                    cMax >= 0 && cMax < v.size() ? numero(v.get(cMax)) : null,
                    cMin >= 0 && cMin < v.size() ? numero(v.get(cMin)) : null, ur, nome()));
        }
        return dias;
    }

    private interface Teste { boolean ok(String coluna); }

    private static int indice(List<String> colunas, Teste teste) {
        for (int i = 0; i < colunas.size(); i++) {
            if (teste.ok(normalizar(colunas.get(i)))) return i;
        }
        return -1;
    }

    static LocalDate data(String valor) {
        String v = valor.trim();
        for (DateTimeFormatter f : DATAS) {
            try {
                return LocalDate.parse(v, f.withResolverStyle(ResolverStyle.STRICT));
            } catch (Exception ignorada) {
                // tenta o próximo formato
            }
        }
        return null;
    }

    /** "0000 UTC", "1200 UTC", "12:00", "12" → hora (0 a 23). */
    static Integer hora(String valor) {
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.isEmpty()) return null;
        int h = Integer.parseInt(digitos.length() >= 3 ? digitos.substring(0, digitos.length() - 2) : digitos);
        return h >= 0 && h <= 23 ? h : null;
    }

    /** "22,4", ",8", "22.4" → número; vazio ou -9999 → null. */
    static Double numero(String valor) {
        String v = valor.trim().replace(',', '.');
        if (v.isEmpty()) return null;
        if (v.startsWith(".")) v = "0" + v;
        if (v.startsWith("-.")) v = "-0" + v.substring(1);
        try {
            double d = Double.parseDouble(v);
            return d <= -9999 ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> separar(String linha) {
        String sep = linha.contains(";") ? ";" : ",";
        List<String> campos = new ArrayList<>();
        for (String c : linha.split(sep, -1)) campos.add(c.trim().replace("\"", ""));
        return campos;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).trim();
    }
}
