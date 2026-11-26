package com.bovix.service;

import com.bovix.calculo.LeitorCsvProducao;
import com.bovix.calculo.LeitorCsvProducao.LinhaProducao;
import com.bovix.calculo.LeitorCsvProducao.LinhaRejeitada;
import com.bovix.model.Fazenda;
import com.bovix.model.Ordenha;
import com.bovix.repository.OrdenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Importa a produção diária do rebanho a partir de um CSV "data;litros".
 * Cada linha vira o total do rebanho daquele dia ("Dia todo"), substituindo o total do rebanho
 * que já existir no dia. Dias com ordenhas por animal são rejeitados (não misturar as duas formas).
 */
@Service
@RequiredArgsConstructor
public class ImportacaoProducaoService {

    public static final long TAMANHO_MAXIMO = 1024 * 1024; // 1 MB

    public record Resumo(int importadas, int substituidas, List<LinhaRejeitada> rejeitadas) {}

    private final OrdenhaRepository repository;
    private final FazendaService fazendaService;
    private final Clock clock;

    @Transactional
    public Resumo importar(Long fazendaId, MultipartFile arquivo) throws IOException {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie um arquivo CSV com as colunas data e litros.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("Arquivo muito grande (máximo 1 MB).");
        }

        LeitorCsvProducao.Resultado lido;
        try {
            lido = LeitorCsvProducao.ler(decodificar(arquivo.getBytes()));
        } catch (IllegalStateException e) {
            throw new RegraNegocioException(e.getMessage());
        }

        List<LinhaRejeitada> rejeitadas = new ArrayList<>(lido.rejeitadas());
        List<LinhaProducao> validas = lido.validas();
        if (validas.isEmpty()) {
            rejeitadas.sort(Comparator.comparingInt(LinhaRejeitada::linha));
            return new Resumo(0, 0, rejeitadas);
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDate min = validas.stream().map(LinhaProducao::data).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = validas.stream().map(LinhaProducao::data).max(LocalDate::compareTo).orElseThrow();
        Map<LocalDate, List<Ordenha>> existentes = repository.findByFazenda_IdAndDataBetweenOrderByDataDesc(fazendaId, min, max)
                .stream().collect(Collectors.groupingBy(Ordenha::getData));

        int importadas = 0;
        int substituidas = 0;
        List<Ordenha> novas = new ArrayList<>();
        for (LinhaProducao linha : validas) {
            if (linha.data().isAfter(hoje)) {
                rejeitadas.add(new LinhaRejeitada(linha.linha(), linha.data() + ";" + linha.litros(), "data no futuro"));
                continue;
            }
            List<Ordenha> doDia = existentes.getOrDefault(linha.data(), List.of());
            if (doDia.stream().anyMatch(o -> o.getAnimal() != null)) {
                rejeitadas.add(new LinhaRejeitada(linha.linha(), linha.data() + ";" + linha.litros(),
                        "o dia já tem ordenhas por animal; remova-as para importar o total do rebanho"));
                continue;
            }
            if (!doDia.isEmpty()) {
                repository.deleteAll(doDia);
                substituidas++;
            } else {
                importadas++;
            }
            novas.add(Ordenha.builder().fazenda(fazenda).data(linha.data()).periodo("Dia todo")
                    .litros(linha.litros()).estimado(false).diasColeta(1).build());
        }
        repository.flush();
        repository.saveAll(novas);

        rejeitadas.sort(Comparator.comparingInt(LinhaRejeitada::linha));
        return new Resumo(importadas, substituidas, rejeitadas);
    }

    /** UTF-8 (com ou sem BOM); se não for UTF-8 válido, lê como Windows-1252 (planilhas do Excel). */
    static String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, java.nio.charset.Charset.forName("windows-1252"));
        }
    }
}
