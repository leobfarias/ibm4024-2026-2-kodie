package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoFerias;

import java.util.List;

/**
 * Situacao consolidada de ferias de uma pessoa: o que o painel exibe em
 * cada linha.
 */
public record SituacaoDeFerias(Pessoa pessoa,
                               SituacaoFerias situacao,
                               int saldoTotal,
                               List<SaldoDePeriodo> periodos) {

    /** Primeiro prazo a vencer entre os periodos com saldo, ou nulo se nao houver. */
    public SaldoDePeriodo getPeriodoMaisUrgente() {
        return periodos.stream()
                .filter(p -> p.saldo() > 0)
                .findFirst()
                .orElse(null);
    }
}
