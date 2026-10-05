package com.ibmec.kodie.model;

/**
 * Situacao do vinculo conforme a secao 2.2 do documento de decisoes.
 */
public enum SituacaoVinculo {

    ATIVO("Ativo"),
    AFASTADO("Afastado"),
    DESLIGADO("Desligado"),
    INATIVO("Inativo");

    private final String descricao;

    SituacaoVinculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
