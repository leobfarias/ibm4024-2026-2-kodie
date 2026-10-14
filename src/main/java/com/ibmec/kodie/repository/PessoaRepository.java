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
     * Vinculo e situacao nulos desligam o proprio criterio. A busca, ao
     * contrario, NUNCA chega nula: o PessoaService entrega o padrao pronto,
     * em minusculas e com os curingas ("%" quando nao ha busca). Um texto
     * nulo dentro de lower() faz o PostgreSQL tratar o parametro como
     * binario e recusar a consulta - erro que o H2 nao reproduz.
     */
    @Query("""
            select p from Pessoa p
            where (lower(p.nome)  like :padrao
                   or lower(p.email) like :padrao
                   or lower(p.cargo) like :padrao)
              and (:tipoVinculo is null or p.tipoVinculo = :tipoVinculo)
              and (:situacao    is null or p.situacao    = :situacao)
            order by p.nome asc
            """)
    List<Pessoa> buscarComFiltros(@Param("padrao") String padrao,
                                  @Param("tipoVinculo") TipoVinculo tipoVinculo,
                                  @Param("situacao") SituacaoVinculo situacao);

    long countBySituacao(SituacaoVinculo situacao);
}
