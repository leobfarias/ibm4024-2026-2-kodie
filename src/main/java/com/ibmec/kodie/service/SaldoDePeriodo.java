package com.ibmec.kodie.service;

import com.ibmec.kodie.model.PeriodoAquisitivo;

import java.time.LocalDate;

/**
 * Um periodo aquisitivo com os numeros calculados no momento da consulta.
 * Nada disto esta armazenado no banco.
 */
public record SaldoDePeriodo(PeriodoAquisitivo periodo,
                             int diasGozados,
                             int saldo,
                             boolean vencido,
                             boolean proximoDoVencimento,
                             long diasAteOLimite) {

    public LocalDate getDataLimiteGozo() {
        return periodo.getDataLimiteGozo();
    }
}
