package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.SituacaoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    List<Ocorrencia> findByPessoaIdOrderByDataInicioDesc(Long pessoaId);

    long countByPessoaId(Long pessoaId);

    /**
     * Ocorrencias que tocam o intervalo [inicio, fim] - inclusive as que
     * comecaram antes dele ou terminam depois, como ferias que atravessam a
     * virada do mes. As canceladas ficam de fora.
     */
    @Query("""
            select o from Ocorrencia o left join fetch o.pessoa
            where o.dataInicio <= :fim
              and o.dataFim    >= :inicio
              and o.situacao   <> :cancelada
            order by o.dataInicio asc, o.id asc
            """)
    List<Ocorrencia> buscarNoPeriodo(@Param("inicio") LocalDate inicio,
                                     @Param("fim") LocalDate fim,
                                     @Param("cancelada") SituacaoOcorrencia cancelada);

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
