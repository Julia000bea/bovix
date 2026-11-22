package com.bovix.service;

import com.bovix.model.Usuario;
import com.bovix.repository.RedefinicaoSenhaRepository;
import com.bovix.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * "Esqueci minha senha": envia por e-mail um link de uso único que vale por 1 hora.
 *
 * - A resposta é sempre a mesma, exista ou não a conta (não revela quais e-mails estão cadastrados),
 *   e o e-mail sai em segundo plano para o tempo de resposta também não denunciar.
 * - O banco guarda só o hash SHA-256 do token; quem ler o banco não consegue usar os links.
 * - O token vai no "#" da URL do frontend, que o navegador não envia para servidor nenhum.
 * - Trocar a senha invalida os logins (JWT) emitidos antes, em todos os aparelhos.
 */
@Service
public class RedefinicaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RedefinicaoSenhaService.class);
    private static final Duration VALIDADE = Duration.ofHours(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RedefinicaoSenhaRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LimiteEnvios limiteEnvios;
    private final String frontendUrl;

    public RedefinicaoSenhaService(UsuarioRepository usuarioRepository,
                                   RedefinicaoSenhaRepository repository,
                                   PasswordEncoder passwordEncoder,
                                   EmailService emailService,
                                   LimiteEnvios limiteEnvios,
                                   @Value("${bovix.frontend-url}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.limiteEnvios = limiteEnvios;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void solicitar(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim().toLowerCase(Locale.ROOT);
        // no máximo 3 links por e-mail por hora (o excedente é ignorado em silêncio)
        if (!limiteEnvios.permitir("redefinir-senha:" + email, 3, Duration.ofHours(1))) {
            log.info("Pedido de redefinição de senha ignorado: limite por hora atingido");
            return;
        }
        Optional<Usuario> usuario = usuarioRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email);
        if (usuario.isEmpty()) return;

        String token = gerarToken();
        long agora = System.currentTimeMillis();
        repository.criarToken(usuario.get().getId(), hash(token), agora + VALIDADE.toMillis(), agora);

        String link = frontendUrl + "/#redefinir=" + token;
        String destino = usuario.get().getEmail();
        String nome = usuario.get().getNome();
        CompletableFuture.runAsync(() -> {
            try {
                emailService.enviarRedefinicaoSenha(destino, nome, link);
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de redefinição de senha do usuário {}", usuario.get().getId(), e);
            }
        });
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        long agora = System.currentTimeMillis();
        Long usuarioId = repository.consumirToken(hash(token.trim()), agora)
                .orElseThrow(() -> new RegraNegocioException(
                        "Link inválido ou expirado. Peça um novo em \"Esqueci minha senha\"."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Conta não encontrada."));
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        repository.registrarTrocaDeSenha(usuarioId, agora);
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
