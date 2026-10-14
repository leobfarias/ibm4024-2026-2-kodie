package com.ibmec.kodie.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lancada quando um recurso solicitado por id nao existe.
 *
 * Na API, o ManipuladorDeErros a traduz em 404 com JSON. Nas telas, o
 * @ResponseStatus faz o Spring exibir a pagina de erro com o mesmo 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
