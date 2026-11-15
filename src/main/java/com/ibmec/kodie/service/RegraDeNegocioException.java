package com.ibmec.kodie.service;

/**
 * Regra de negocio violada, junto com o campo do formulario a que ela se refere.
 *
 * Estende IllegalArgumentException de proposito: a API continua tratando o
 * erro exatamente como antes (400 no ManipuladorDeErros). As telas usam o
 * campo para mostrar a mensagem ao lado do lugar certo, e nao sempre no CPF.
 */
public class RegraDeNegocioException extends IllegalArgumentException {

    private final String campo;

    public RegraDeNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
