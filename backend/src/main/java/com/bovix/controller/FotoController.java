package com.bovix.controller;

import com.bovix.repository.AnimalFotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Serve as fotos dos animais. É público (a tag <img> não envia o token JWT), mas a URL
 * contém uma chave aleatória (UUID) que só quem tem acesso ao animal recebe — o mesmo
 * modelo que já valia para /uploads. A chave muda a cada novo envio, então dá para
 * guardar a foto em cache por muito tempo.
 */
@RestController
@RequestMapping("/api/fotos")
@RequiredArgsConstructor
public class FotoController {

    private static final Pattern CHAVE_VALIDA = Pattern.compile("^[0-9a-f-]{36}$");

    private final AnimalFotoRepository fotoRepository;

    @GetMapping("/{chave}")
    public ResponseEntity<byte[]> buscar(@PathVariable String chave) {
        if (!CHAVE_VALIDA.matcher(chave).matches()) {
            return ResponseEntity.notFound().build();
        }
        return fotoRepository.buscarPorChave(chave)
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.contentType()))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .header("X-Content-Type-Options", "nosniff")
                        .body(foto.conteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
