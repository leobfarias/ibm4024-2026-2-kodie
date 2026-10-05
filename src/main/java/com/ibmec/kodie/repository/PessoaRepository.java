package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    /** Usado na criacao: o CPF nao pode pertencer a ninguem. */
    boolean existsByCpf(String cpf);

    /** Usado na atualizacao: o CPF so e problema se pertencer a OUTRA pessoa. */
    boolean existsByCpfAndIdNot(String cpf, Long id);
}
