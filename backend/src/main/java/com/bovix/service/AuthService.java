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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FazendaRepository fazendaRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LimiteTentativasLogin limiteTentativas;

    // usuário, fazenda e configuração nascem juntos: se um falhar, nada fica gravado pela metade
    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String email = normalizarEmail(req.getEmail());
        if (usuarioRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email).isPresent()) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = Usuario.builder()
                .nome(req.getNome().trim())
                .email(email)
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
        String email = normalizarEmail(req.getEmail());
        limiteTentativas.verificar(email);

        Usuario usuario = usuarioRepository.findByEmail(req.getEmail().trim())
                .or(() -> usuarioRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email))
                .orElse(null);
        if (usuario == null || !passwordEncoder.matches(req.getSenha(), usuario.getSenhaHash())) {
            limiteTentativas.registrarFalha(email);
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }
        limiteTentativas.registrarSucesso(email);

        List<Fazenda> fazendas = fazendaRepository.findByUsuario_Id(usuario.getId());
        return montarResposta(usuario, fazendas);
    }

    public AuthResponse me(Usuario usuario) {
        List<Fazenda> fazendas = fazendaRepository.findByUsuario_Id(usuario.getId());
        return montarResposta(usuario, fazendas);
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
