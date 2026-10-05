package com.ibmec.kodie.model;

/**
 * Tipos de vinculo previstos na secao 2.2 do documento de decisoes.
 * Apenas CLT segue as regras trabalhistas de ferias (secao 2.4).
 */
public enum TipoVinculo {

    CLT("CLT"),
    MEI("MEI"),
    PJ("PJ"),
    VOLUNTARIO("Voluntario");

    private final String descricao;

    TipoVinculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Define se a pessoa acumula periodo aquisitivo de ferias. */
    public boolean temRegraDeFerias() {
        return this == CLT;
    }
}
