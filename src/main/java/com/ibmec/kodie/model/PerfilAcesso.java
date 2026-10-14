package com.ibmec.kodie.model;

/**
 * Perfis de acesso ao sistema (secao 2.2 do documento de decisoes).
 *
 * ADMIN cadastra, altera e remove. VISUALIZADOR apenas consulta: ve as
 * mesmas telas, sem os formularios e sem as acoes de escrita.
 */
public enum PerfilAcesso {

    ADMIN("Administrador"),
    VISUALIZADOR("Visualização");

    private final String descricao;

    PerfilAcesso(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
