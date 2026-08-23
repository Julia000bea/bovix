package com.bovix.service;

import com.bovix.dto.AuthResponse;
import com.bovix.dto.LoginRequest;
import com.bovix.dto.RegistroRequest;
import com.bovix.model.Configuracao;
import com.bovix.model.Fazenda;
import com.bovix.model.Usuario;
import com.bovix.repository.ConfiguracaoRepository;
import com.bovix.repository.FazendaRepository;
import com.bovix.repository.UsuarioRepository;
import com.bovix.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FazendaRepository fazendaRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registrar(RegistroRequest req) {
        if (usuarioRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = Usuario.builder()
                .nome(req.getNome())
                .email(req.getEmail())
                .senhaHash(passwordEncoder.encode(req.getSenha()))
                .build();
        usuario = usuarioRepository.save(usuario);

        Fazenda fazenda = Fazenda.builder()
                .usuario(usuario)
                .nome(req.getFazendaNome())
                .cidade(req.getCidade())
                .estado(req.getEstado())
                .build();
        fazenda = fazendaRepository.save(fazenda);

        Configuracao config = Configuracao.builder().fazenda(fazenda).gestacaoDias(283).cioDias(21).build();
        configuracaoRepository.save(config);

        return montarResposta(usuario, List.of(fazenda));
    }

    public AuthResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(req.getSenha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        List<Fazenda> fazendas = fazendaRepository.findByUsuario_Id(usuario.getId());
        return montarResposta(usuario, fazendas);
    }

    public AuthResponse me(Usuario usuario) {
        List<Fazenda> fazendas = fazendaRepository.findByUsuario_Id(usuario.getId());
        return montarResposta(usuario, fazendas);
    }

    private AuthResponse montarResposta(Usuario usuario, List<Fazenda> fazendas) {
        String token = jwtService.gerarToken(usuario.getId(), usuario.getEmail());
        var usuarioResumo = new AuthResponse.UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail());
        var fazendasResumo = fazendas.stream()
                .map(f -> new AuthResponse.FazendaResumo(f.getId(), f.getNome()))
                .toList();
        return new AuthResponse(token, usuarioResumo, fazendasResumo);
    }
}
