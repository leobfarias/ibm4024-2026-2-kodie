package com.ibmec.kodie.service.indicadores;

import com.ibmec.kodie.model.TipoOcorrencia;

/**
 * Ocorrencias de um tipo no ano.
 *
 * @param dias       dias corridos dentro do ano - ferias que comecam em dezembro
 *                   contam so os dias de dezembro
 * @param percentual fracao dos dias deste tipo sobre o total de dias do ano
 */
public record OcorrenciasPorTipo(TipoOcorrencia tipo, long quantidade, long dias, int percentual) {
}
