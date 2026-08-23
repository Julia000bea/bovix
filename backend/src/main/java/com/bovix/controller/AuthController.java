package com.bovix.controller;

import com.bovix.dto.AuthResponse;
import com.bovix.dto.LoginRequest;
import com.bovix.dto.RegistroRequest;
import com.bovix.security.SecurityUtils;
import com.bovix.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}
