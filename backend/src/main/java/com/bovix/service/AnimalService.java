package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.repository.AnimalRepository;
import com.bovix.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final FazendaService fazendaService;

    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    @Value("${bovix.uploads-dir}")
    private String uploadsDir;

    public List<Animal> listarPorFazenda(Long fazendaId) {
        fazendaService.verificarPropriedade(fazendaId);
        return animalRepository.findByFazenda_IdOrderByBrincoAsc(fazendaId);
    }

    public Animal buscar(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal não encontrado: " + id));
        verificarPropriedade(animal);
        return animal;
    }

    public Animal criar(Long fazendaId, Animal dados) {
        Fazenda fazenda = fazendaService.verificarPropriedade(fazendaId);
        dados.setFazenda(fazenda);
        return animalRepository.save(dados);
    }

    public Animal atualizar(Long id, Animal dados) {
        Animal existente = buscar(id); // já valida propriedade
        existente.setBrinco(dados.getBrinco());
        existente.setNome(dados.getNome());
        existente.setRaca(dados.getRaca());
        existente.setNascimento(dados.getNascimento());
        existente.setCategoria(dados.getCategoria());
        existente.setStatus(dados.getStatus());
        existente.setNotas(dados.getNotas());
        return animalRepository.save(existente);
    }

    public void remover(Long id) {
        buscar(id); // valida propriedade antes de remover
        animalRepository.deleteById(id);
    }

    /** Salva a foto enviada em disco e associa a URL pública ao animal. */
    public Animal salvarFoto(Long id, MultipartFile arquivo) throws IOException {
        Animal animal = buscar(id); // já valida propriedade

        // extensão vem do tipo validado, nunca do nome enviado — evita hospedar .html/.svg com script
        String extensao = arquivo.isEmpty() ? null : EXTENSAO_POR_TIPO.get(arquivo.getContentType());
        if (extensao == null || !assinaturaDeImagem(arquivo)) {
            throw new RegraNegocioException("Envie uma imagem JPG, PNG ou WEBP.");
        }

        Path pasta = Paths.get(uploadsDir);
        Files.createDirectories(pasta);

        String nomeArquivo = UUID.randomUUID() + extensao;
        Path destino = pasta.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        animal.setFotoUrl("/uploads/animais/" + nomeArquivo);
        return animalRepository.save(animal);
    }

    /** Usado por outros services (reprodução, saúde) para validar o animal antes de criar eventos.
     *  Consulta direta ao banco (não navega animal.getFazenda().getUsuario()) para não estourar
     *  LazyInitializationException fora de uma sessão do Hibernate. */
    public void verificarPropriedade(Animal animal) {
        Long usuarioId = SecurityUtils.usuarioAtual().getId();
        if (!animalRepository.existsByIdAndFazenda_Usuario_Id(animal.getId(), usuarioId)) {
            throw new AccessDeniedException("Este animal não pertence a uma fazenda do usuário autenticado.");
        }
    }

    /** Confere os primeiros bytes (JPEG, PNG ou WEBP) — o content-type sozinho é informado pelo cliente. */
    private boolean assinaturaDeImagem(MultipartFile arquivo) throws IOException {
        byte[] b = new byte[12];
        int lidos;
        try (InputStream in = arquivo.getInputStream()) {
            lidos = in.readNBytes(b, 0, b.length);
        }
        if (lidos >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true; // JPEG
        if (lidos >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return true;      // PNG
        return lidos >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';                                   // WEBP
    }
}
