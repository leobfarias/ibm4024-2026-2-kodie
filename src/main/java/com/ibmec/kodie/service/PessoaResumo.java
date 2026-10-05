package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Pessoa;

/**
 * Linha da listagem: a pessoa somada ao total de ocorrencias dela.
 *
 * O relacionamento e unidirecional, entao Pessoa nao conhece suas ocorrencias.
 * O Service monta a contagem e entrega pronta, em vez de o template navegar
 * pelo relacionamento.
 */
public record PessoaResumo(Pessoa pessoa, long totalOcorrencias) {
}
