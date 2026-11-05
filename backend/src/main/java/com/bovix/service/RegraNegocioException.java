package com.bovix.service;

/** Dado recebido viola uma regra de negócio — vira HTTP 400 com a mensagem para o usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
