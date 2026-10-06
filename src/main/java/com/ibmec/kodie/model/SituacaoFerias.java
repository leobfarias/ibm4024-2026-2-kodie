package com.ibmec.kodie.model;

/**
 * Os quatro grupos do painel de ferias (secao 3.2 do documento de decisoes):
 * os tres estados pedidos pela cliente mais o grupo das pessoas sem regra CLT.
 */
public enum SituacaoFerias {

    REGULAR("Regular"),
    PROXIMO_DO_VENCIMENTO("Proximo do vencimento"),
    VENCIDO("Vencido"),
    SEM_REGRA("Sem regra CLT aplicavel");

    private final String descricao;

    SituacaoFerias(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
