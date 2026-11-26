package com.bovix.clima;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados horários das estações automáticas do INMET (apitempo.inmet.gov.br).
 *
 * Não há documentação oficial pública e atual da API. O formato abaixo segue o que bibliotecas
 * de terceiros que consomem a API descrevem (ver README / relatório do TCC):
 * - rota com token: {url}/token/estacao/{AAAA-MM-DD}/{AAAA-MM-DD}/{codigo}/{token};
 * - resposta: lista JSON com DT_MEDICAO (AAAA-MM-DD), HR_MEDICAO (HHMM, em UTC), TEM_INS, UMD_INS,
 *   TEM_MAX e TEM_MIN (texto ou null).
 * O token é pedido ao INMET (cadastro.act@inmet.gov.br) e vem de BOVIX_INMET_TOKEN.
 * Sem token, a fonte fica indisponível e o clima entra pela importação de CSV.
 */
@Component
public class InmetFonteClima implements FonteClima {

    private final String url;
    private final String token;
    private final ZoneId zona;
    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();

    public InmetFonteClima(@Value("${bovix.inmet.url}") String url,
                           @Value("${bovix.inmet.token:}") String token,
                           @Value("${bovix.inmet.timeout-segundos:20}") int timeoutSegundos,
                           Clock clock) {
        this.url = url.replaceAll("/+$", "");
        this.token = token == null ? "" : token.trim();
        this.zona = clock.getZone();
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public String nome() {
        return "inmet";
    }

    /** Sem token configurado, a busca automática fica desligada. */
    public boolean disponivel() {
        return !token.isEmpty();
    }

    @Override
    public List<ClimaDiario> buscar(String estacao, LocalDate inicio, LocalDate fim) throws FonteClimaException {
        if (!disponivel()) throw new FonteClimaException("Token do INMET não configurado (BOVIX_INMET_TOKEN).");
        if (estacao == null || !estacao.matches("[A-Z]\\d{3}")) throw new FonteClimaException("Código de estação inválido: " + estacao);

        // a hora vem em UTC: o dia local termina no dia seguinte em UTC (ex.: 23h em Brasília = 02h UTC)
        String corpo;
        try {
            corpo = http.get()
                    .uri(url + "/token/estacao/{ini}/{fim}/{estacao}/{token}", inicio, fim.plusDays(1), estacao, token)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new FonteClimaException("INMET indisponível para a estação " + estacao + ": " + semToken(e.getMessage()));
        }

        List<LeituraHoraria> leituras = lerResposta(corpo);
        return AgregadorClima.agregar(leituras, estacao, nome(), zona).stream()
                .filter(d -> !d.data().isBefore(inicio) && !d.data().isAfter(fim))
                .toList();
    }

    /** Converte a resposta JSON do INMET em leituras horárias (instante em UTC). */
    List<LeituraHoraria> lerResposta(String corpo) throws FonteClimaException {
        if (corpo == null || corpo.isBlank()) return List.of(); // sem dados no período
        JsonNode raiz;
        try {
            raiz = json.readTree(corpo);
        } catch (Exception e) {
            throw new FonteClimaException("Resposta do INMET em formato inesperado.");
        }
        if (!raiz.isArray()) throw new FonteClimaException("Resposta do INMET em formato inesperado.");

        List<LeituraHoraria> leituras = new ArrayList<>();
        for (JsonNode item : raiz) {
            try {
                LocalDate data = LocalDate.parse(item.path("DT_MEDICAO").asText());
                // "0000", "1200" (HHMM) ou "12"
                String hhmm = item.path("HR_MEDICAO").asText("").replaceAll("\\D", "");
                if (hhmm.isEmpty()) continue;
                int hora = Integer.parseInt(hhmm.length() >= 3 ? hhmm.substring(0, hhmm.length() - 2) : hhmm);
                Instant instante = data.atTime(LocalTime.of(hora, 0)).toInstant(ZoneOffset.UTC);
                leituras.add(new LeituraHoraria(instante, numero(item, "TEM_INS"), numero(item, "UMD_INS"),
                        numero(item, "TEM_MAX"), numero(item, "TEM_MIN")));
            } catch (RuntimeException ignorada) {
                // linha com data/hora inválida: ignora só ela
            }
        }
        return leituras;
    }

    private static Double numero(JsonNode item, String campo) {
        JsonNode n = item.get(campo);
        if (n == null || n.isNull()) return null;
        String texto = n.asText().trim().replace(',', '.');
        if (texto.isEmpty()) return null;
        try {
            double v = Double.parseDouble(texto);
            return v <= -9999 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Remove o token de mensagens de erro (exceções do cliente HTTP incluem a URL). */
    String semToken(String mensagem) {
        if (mensagem == null) return "erro desconhecido";
        return token.isEmpty() ? mensagem : mensagem.replace(token, "***");
    }
}
