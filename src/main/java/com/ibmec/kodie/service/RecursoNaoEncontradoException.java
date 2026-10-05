package com.ibmec.kodie.service;

/**
 * Lancada quando um recurso solicitado por id nao existe.
 * O ManipuladorDeErros traduz esta excecao em HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
