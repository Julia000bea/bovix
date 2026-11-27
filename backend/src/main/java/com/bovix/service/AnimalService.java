package com.bovix.service;

import com.bovix.model.Animal;
import com.bovix.model.Fazenda;
import com.bovix.repository.AnimalFotoRepository;
import com.bovix.repository.AnimalRepository;
import com.bovix.repository.EventoReproducaoRepository;
import com.bovix.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;
    private final FazendaService fazendaService;
    private final EventoReproducaoRepository reproducaoRepository;
    private final Clock clock;

    private static final Set<String> TIPOS_IMAGEM = Set.of("image/jpeg", "image/png", "image/webp");
    // o frontend reduz a foto antes de enviar; isto só barra arquivos que não passaram por ele
    private static final long TAMANHO_MAXIMO_FOTO = 3L * 1024 * 1024;

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
        validarCategoria(dados.getCategoria());
        definirDataSaida(dados, dados.getDataSaida(), null);
        return animalRepository.save(dados);
    }

    public Animal atualizar(Long id, Animal dados) {
        Animal existente = buscar(id); // já valida propriedade
        existente.setBrinco(dados.getBrinco());
        existente.setNome(dados.getNome());
        existente.setRaca(dados.getRaca());
        existente.setNascimento(dados.getNascimento());
        validarCategoria(dados.getCategoria());
        if (Animal.CATEGORIAS_MACHO.contains(dados.getCategoria()) && !existente.ehMacho()
                && reproducaoRepository.existsByAnimal_Id(existente.getId())) {
            throw new RegraNegocioException("Este animal tem eventos de reprodução (cio, cobertura, parto...). "
                    + "Remova-os antes de mudar a categoria para macho.");
        }
        existente.setCategoria(dados.getCategoria());
        existente.setStatus(dados.getStatus());
        existente.setNotas(dados.getNotas());
        definirDataSaida(existente, dados.getDataSaida(), existente.getDataSaida());
        return animalRepository.save(existente);
    }

    /**
     * Ativa: sem data de saída. Vendida/Morta: usa a data informada; sem ela, mantém a que já
     * existia ou, se não havia, usa hoje (o dia em que o status foi alterado).
     */
    void definirDataSaida(Animal animal, LocalDate informada, LocalDate anterior) {
        if (animal.getStatus() == null || "Ativa".equals(animal.getStatus())) {
            animal.setDataSaida(null);
            return;
        }
        LocalDate hoje = LocalDate.now(clock);
        LocalDate data = informada != null ? informada : anterior != null ? anterior : hoje;
        if (data.isAfter(hoje)) {
            throw new RegraNegocioException("A data de saída não pode estar no futuro.");
        }
        if (animal.getNascimento() != null && data.isBefore(animal.getNascimento())) {
            throw new RegraNegocioException("A data de saída não pode ser anterior ao nascimento.");
        }
        animal.setDataSaida(data);
    }

    static void validarCategoria(String categoria) {
        if (categoria == null || !(Animal.CATEGORIAS_FEMEA.contains(categoria) || Animal.CATEGORIAS_MACHO.contains(categoria))) {
            throw new RegraNegocioException("Categoria inválida: " + categoria
                    + ". Use Bezerra, Novilha, Vaca lactante, Vaca seca, Bezerro ou Touro.");
        }
    }

    public void remover(Long id) {
        buscar(id); // valida propriedade antes de remover
        animalRepository.deleteById(id);
    }

    /**
     * Salva a foto no banco (tabela animal_fotos) e associa ao animal a URL pública /api/fotos/{chave}.
     * Não usa o disco do servidor: em hospedagens como o Render ele é apagado a cada deploy/reinício.
     */
    @Transactional
    public Animal salvarFoto(Long id, MultipartFile arquivo) throws IOException {
        Animal animal = buscar(id); // já valida propriedade

        String tipo = arquivo.getContentType();
        if (arquivo.isEmpty() || tipo == null || !TIPOS_IMAGEM.contains(tipo)) {
            throw new RegraNegocioException("Envie uma imagem JPG, PNG ou WEBP.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_FOTO) {
            throw new RegraNegocioException("Foto muito grande (máximo 3 MB).");
        }
        byte[] conteudo = arquivo.getBytes();
        if (!assinaturaDeImagem(conteudo)) {
            throw new RegraNegocioException("Envie uma imagem JPG, PNG ou WEBP.");
        }

        String chave = UUID.randomUUID().toString();
        animalFotoRepository.salvar(animal.getId(), chave, tipo, conteudo);

        animal.setFotoUrl("/api/fotos/" + chave);
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
    private boolean assinaturaDeImagem(byte[] b) {
        int lidos = b.length;
        if (lidos >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true; // JPEG
        if (lidos >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return true;      // PNG
        return lidos >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';                                   // WEBP
    }
}
