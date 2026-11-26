package com.bovix.config;

import com.bovix.service.LimiteCadastros;
import com.bovix.service.LimiteTentativasLogin;
import com.bovix.service.RegraNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converte exceções de negócio/segurança em respostas HTTP consistentes para o frontend.
 * Nunca devolve a mensagem técnica de exceções inesperadas (SQL, nomes de tabela, stack):
 * ela vai só para o log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarValidacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Dados inválidos." : mensagem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> tratarValidacaoEntidade(ConstraintViolationException e) {
        String mensagem = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Dados inválidos." : mensagem);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> tratarRegraNegocio(RegraNegocioException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> tratarRequisicaoInvalida(Exception e) {
        return erro(HttpStatus.BAD_REQUEST, "Requisição inválida: confira os campos enviados (datas, números e parâmetros).");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(IllegalArgumentException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> tratarRotaInexistente(NoResourceFoundException e) {
        return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> tratarMetodo(HttpRequestMethodNotSupportedException e) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado nesta rota.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> tratarIntegridade(DataIntegrityViolationException e) {
        String detalhe = String.valueOf(e.getMostSpecificCause().getMessage());
        if (detalhe.contains("uq_animal_brinco_por_fazenda")) {
            return erro(HttpStatus.CONFLICT, "Já existe um animal com este brinco nesta fazenda.");
        }
        if (detalhe.contains("usuarios_email_key")) {
            return erro(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }
        log.warn("Violação de integridade: {}", detalhe);
        return erro(HttpStatus.CONFLICT, "Os dados não puderam ser salvos: algum valor está fora do permitido ou duplicado.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tratarUploadGrande(MaxUploadSizeExceededException e) {
        return erro(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo muito grande (máximo 5 MB).");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> tratarMultipart(MultipartException e) {
        return erro(HttpStatus.BAD_REQUEST, "Envio de arquivo inválido.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> tratarAcessoNegado(AccessDeniedException e) {
        return erro(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> tratarCredenciais(BadCredentialsException e) {
        return erro(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(LimiteTentativasLogin.MuitasTentativasException.class)
    public ResponseEntity<Map<String, String>> tratarMuitasTentativas(LimiteTentativasLogin.MuitasTentativasException e) {
        return erro(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(LimiteCadastros.MuitosCadastrosException.class)
    public ResponseEntity<Map<String, String>> tratarMuitosCadastros(LimiteCadastros.MuitosCadastrosException e) {
        return erro(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> tratarGenerico(Exception e) {
        log.error("Erro inesperado na requisição", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor. Tente novamente em instantes.");
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
