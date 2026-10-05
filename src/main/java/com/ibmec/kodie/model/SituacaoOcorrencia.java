package com.ibmec.kodie.model;

public enum SituacaoOcorrencia {

    PLANEJADA("Planejada"),
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada");

    private final String descricao;

    SituacaoOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
