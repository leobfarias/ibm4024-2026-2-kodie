package com.ibmec.kodie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Periodo aquisitivo de ferias, conforme o modelo escolhido na secao 2.3
 * do documento de decisoes.
 *
 * A cada doze meses de vinculo nasce um registro. O saldo NAO e armazenado
 * aqui: ele e calculado no momento da consulta, a partir das ocorrencias do
 * tipo FERIAS. Assim os periodos se acumulam naturalmente, o historico
 * permanece visivel e nenhum dado precisa ser sobrescrito.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pessoa_id", "dataInicio"}))
public class PeriodoAquisitivo {

    /** Dias de direito por periodo completo. */
    public static final int DIAS_POR_PERIODO = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /** Ordem do periodo na vida do vinculo: 1 para o primeiro, 2 para o seguinte. */
    @Column(nullable = false)
    private Integer numero;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate dataInicio;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate dataFim;

    @Column(nullable = false)
    private Integer diasDeDireito = DIAS_POR_PERIODO;

    /**
     * Fim do periodo concessivo: doze meses apos o fim do periodo aquisitivo.
     * Passado esse prazo sem gozo, as ferias estao vencidas.
     */
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate dataLimiteGozo;

    public PeriodoAquisitivo() {
    }

    public PeriodoAquisitivo(Pessoa pessoa, int numero, LocalDate dataInicio) {
        this.pessoa = pessoa;
        this.numero = numero;
        this.dataInicio = dataInicio;
        this.dataFim = dataInicio.plusYears(1).minusDays(1);
        this.diasDeDireito = DIAS_POR_PERIODO;
        this.dataLimiteGozo = this.dataFim.plusYears(1);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getDiasDeDireito() {
        return diasDeDireito;
    }

    public void setDiasDeDireito(Integer diasDeDireito) {
        this.diasDeDireito = diasDeDireito;
    }

    public LocalDate getDataLimiteGozo() {
        return dataLimiteGozo;
    }

    public void setDataLimiteGozo(LocalDate dataLimiteGozo) {
        this.dataLimiteGozo = dataLimiteGozo;
    }
}
