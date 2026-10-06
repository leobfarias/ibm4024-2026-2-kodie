package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.model.TipoVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    /** Usado na criacao: o CPF nao pode pertencer a ninguem. */
    boolean existsByCpf(String cpf);

    /** Usado na atualizacao: o CPF so e problema se pertencer a OUTRA pessoa. */
    boolean existsByCpfAndIdNot(String cpf, Long id);

    /**
     * Consulta da tela de pessoas: busca livre por nome, e-mail ou cargo,
     * combinada com os filtros de vinculo e situacao.
     *
     * Cada parametro nulo desliga o proprio criterio, o que permite uma unica
     * consulta atender todas as combinacoes de filtro da tela.
     */
    @Query("""
            select p from Pessoa p
            where (:busca is null
                   or lower(p.nome)  like lower(concat('%', :busca, '%'))
                   or lower(p.email) like lower(concat('%', :busca, '%'))
                   or lower(p.cargo) like lower(concat('%', :busca, '%')))
              and (:tipoVinculo is null or p.tipoVinculo = :tipoVinculo)
              and (:situacao    is null or p.situacao    = :situacao)
            order by p.nome asc
            """)
    List<Pessoa> buscarComFiltros(@Param("busca") String busca,
                                  @Param("tipoVinculo") TipoVinculo tipoVinculo,
                                  @Param("situacao") SituacaoVinculo situacao);

    long countBySituacao(SituacaoVinculo situacao);
}
