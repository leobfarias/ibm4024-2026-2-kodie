package com.ibmec.kodie.model;

/**
 * As cinco categorias da secao 2.2 do documento de decisoes, tratadas
 * como uma unica entidade por compartilharem a mesma estrutura.
 */
public enum TipoOcorrencia {

    FERIAS("Ferias"),
    FOLGA("Folga"),
    LICENCA("Licenca"),
    AUSENCIA("Ausencia"),
    FERIADO_TRABALHADO("Feriado trabalhado");

    private final String descricao;

    TipoOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
