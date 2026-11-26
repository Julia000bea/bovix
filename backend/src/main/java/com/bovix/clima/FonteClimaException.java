package com.bovix.clima;

/** Falha ao obter dados de clima. A mensagem nunca contém o token do INMET. */
public class FonteClimaException extends Exception {
    public FonteClimaException(String mensagem) {
        super(mensagem);
    }
}
