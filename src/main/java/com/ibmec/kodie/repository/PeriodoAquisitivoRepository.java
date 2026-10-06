package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.PeriodoAquisitivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodoAquisitivoRepository extends JpaRepository<PeriodoAquisitivo, Long> {

    List<PeriodoAquisitivo> findByPessoaIdOrderByDataInicioAsc(Long pessoaId);

    void deleteByPessoaId(Long pessoaId);
}
