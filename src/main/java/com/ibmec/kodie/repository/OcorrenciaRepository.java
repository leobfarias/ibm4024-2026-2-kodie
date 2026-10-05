package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    List<Ocorrencia> findByPessoaIdOrderByDataInicioDesc(Long pessoaId);

    long countByPessoaId(Long pessoaId);

    /**
     * Duas ocorrencias se sobrepoem quando uma comeca antes de a outra terminar
     * e termina depois de a outra comecar. Cancelada nao conta.
     */
    boolean existsByPessoaIdAndSituacaoNotAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            Long pessoaId, com.ibmec.kodie.model.SituacaoOcorrencia situacao,
            LocalDate dataFim, LocalDate dataInicio);

    /** Mesma verificacao, ignorando a propria ocorrencia durante a atualizacao. */
    boolean existsByPessoaIdAndIdNotAndSituacaoNotAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            Long pessoaId, Long id, com.ibmec.kodie.model.SituacaoOcorrencia situacao,
            LocalDate dataFim, LocalDate dataInicio);
}
