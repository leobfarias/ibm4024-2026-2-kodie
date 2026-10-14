package com.ibmec.kodie.service.indicadores;

import com.ibmec.kodie.model.Ocorrencia;

import java.time.LocalDate;
import java.util.List;

/**
 * Tudo o que a tela de indicadores exibe, calculado na hora a partir do
 * cadastro central. Nada aqui e armazenado (secao 2.1 do documento).
 *
 * @param quadroAtual pessoas ativas ou afastadas - quem tem vinculo vigente
 */
public record PainelDeIndicadores(
        LocalDate referencia,
        long totalCadastrado,
        long quadroAtual,
        List<Fatia> porSituacao,
        List<Fatia> porVinculo,
        List<Fatia> feriasPorSituacao,
        long pessoasComFeriasVencidas,
        int saldoDeFeriasTotal,
        int diasDeFeriasVencidos,
        int ano,
        List<OcorrenciasPorTipo> ocorrenciasNoAno,
        long totalDeOcorrenciasNoAno,
        List<Ocorrencia> ausentesHoje,
        List<Ocorrencia> proximasOcorrencias,
        long admissoesUltimos12Meses,
        long desligamentosUltimos12Meses) {
}
