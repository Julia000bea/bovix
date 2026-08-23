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
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final FazendaService fazendaService;

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

        Path pasta = Paths.get(uploadsDir);
        Files.createDirectories(pasta);

        String extensao = obterExtensao(arquivo.getOriginalFilename());
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

    private String obterExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) return "";
        return nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
    }
}
