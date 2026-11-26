package com.bovix.service;

import com.bovix.clima.ClimaDiario;
import com.bovix.clima.CsvFonteClima;
import com.bovix.clima.FonteClimaException;
import com.bovix.dto.LocalizacaoRequest;
import com.bovix.model.Fazenda;
import com.bovix.repository.ClimaRepository;
import com.bovix.repository.FazendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Clima da fazenda: série por período, importação de CSV e localização/estação. */
@Service
@RequiredArgsConstructor
public class ClimaService {

    public static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;

    public record ResumoImportacao(String estacao, int importados, int substituidos, LocalDate primeiroDia, LocalDate ultimoDia) {}

    private final ClimaRepository climaRepository;
    private final FazendaRepository fazendaRepository;
    private final FazendaService fazendaService;
    private final Clock clock;

    public List<ClimaDiario> serie(Long fazendaId, LocalDate inicio, LocalDate fim) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        IndicadoresService.validarPeriodo(inicio, fim);
        if (fazenda.getEstacaoInmet() == null) return List.of();
        return climaRepository.serie(fazenda.getEstacaoInmet(), inicio, fim);
    }

    @Transactional
    public Fazenda atualizarLocalizacao(Long fazendaId, LocalizacaoRequest dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        fazenda.setLatitude(dados.getLatitude() == null ? null : dados.getLatitude().setScale(6, RoundingMode.HALF_UP));
        fazenda.setLongitude(dados.getLongitude() == null ? null : dados.getLongitude().setScale(6, RoundingMode.HALF_UP));
        String estacao = dados.getEstacaoInmet() == null ? "" : dados.getEstacaoInmet().trim().toUpperCase(Locale.ROOT);
        fazenda.setEstacaoInmet(estacao.isEmpty() ? null : estacao);
        return fazendaRepository.save(fazenda);
    }

    /** Importa um CSV histórico de clima para a estação configurada na fazenda (substitui dias existentes). */
    @Transactional
    public ResumoImportacao importar(Long fazendaId, MultipartFile arquivo) throws IOException {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        String estacao = fazenda.getEstacaoInmet();
        if (estacao == null) {
            throw new RegraNegocioException("Informe a estação do INMET nas configurações antes de importar o clima.");
        }
        if (arquivo == null || arquivo.isEmpty()) throw new RegraNegocioException("Envie um arquivo CSV de clima.");
        if (arquivo.getSize() > TAMANHO_MAXIMO) throw new RegraNegocioException("Arquivo muito grande (máximo 5 MB).");

        CsvFonteClima csv;
        try {
            csv = new CsvFonteClima(ImportacaoProducaoService.decodificar(arquivo.getBytes()), clock.getZone());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException(e.getMessage());
        }
        String doArquivo = csv.estacaoDoArquivo();
        if (doArquivo != null && !doArquivo.equals(estacao)) {
            throw new RegraNegocioException("O arquivo é da estação " + doArquivo + ", mas a fazenda usa a estação " + estacao + ".");
        }

        List<ClimaDiario> dias;
        try {
            dias = csv.buscar(estacao, null, LocalDate.now(clock));
        } catch (FonteClimaException e) {
            throw new RegraNegocioException(e.getMessage());
        }
        if (dias.isEmpty()) {
            throw new RegraNegocioException("Nenhum dia completo no arquivo (são necessárias ao menos 18 horas com temperatura e umidade por dia).");
        }
        int substituidos = 0;
        for (ClimaDiario d : dias) {
            if (climaRepository.salvarOuSubstituir(d)) substituidos++;
        }
        return new ResumoImportacao(estacao, dias.size() - substituidos, substituidos,
                dias.get(0).data(), dias.get(dias.size() - 1).data());
    }
}
