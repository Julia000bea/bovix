package com.bovix.controller;

import com.bovix.dto.AuthResponse;
import com.bovix.dto.EsqueciSenhaRequest;
import com.bovix.dto.LoginRequest;
import com.bovix.dto.RedefinirSenhaRequest;
import com.bovix.dto.RegistroRequest;
import com.bovix.security.SecurityUtils;
import com.bovix.service.AuthService;
import com.bovix.service.RedefinicaoSenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RedefinicaoSenhaService redefinicaoSenhaService;

    @PostMapping("/registrar")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.ok(authService.registrar(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Usado pelo frontend para restaurar a sessão a partir do token salvo. */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return ResponseEntity.ok(authService.me(SecurityUtils.usuarioAtual()));
    }

    /** Sempre responde igual, exista ou não a conta — não revela quais e-mails estão cadastrados. */
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest req) {
        redefinicaoSenhaService.solicitar(req.getEmail());
        return ResponseEntity.ok(Map.of("mensagem",
                "Se houver uma conta com este e-mail, enviamos um link para criar uma nova senha. Confira também a caixa de spam."));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        redefinicaoSenhaService.redefinir(req.getToken(), req.getNovaSenha());
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada. Entre com a nova senha."));
    }
}
