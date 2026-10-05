package com.ibmec.kodie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Ferias, folgas, licencas, ausencias e trabalho em feriados (secao 2.2).
 *
 * O relacionamento com Pessoa e unidirecional: apenas este lado conhece
 * o outro. A chave estrangeira fica aqui, no lado "muitos".
 */
@Entity
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O tipo da ocorrencia e obrigatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoOcorrencia tipo;

    @NotNull(message = "A data de inicio e obrigatoria")
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @NotNull(message = "A data de fim e obrigatoria")
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @NotNull(message = "A situacao e obrigatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoOcorrencia situacao = SituacaoOcorrencia.PLANEJADA;

    @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres")
    @Column(length = 255)
    private String observacao;

    /** Pode ser nula: existem ocorrencias de calendario sem pessoa vinculada. */
    @ManyToOne
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    public Ocorrencia() {
    }

    public Ocorrencia(TipoOcorrencia tipo, LocalDate dataInicio, LocalDate dataFim,
                      SituacaoOcorrencia situacao, String observacao, Pessoa pessoa) {
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.situacao = situacao;
        this.observacao = observacao;
        this.pessoa = pessoa;
    }

    /** Numero de dias corridos, usado nas listagens. */
    @Transient
    public long getTotalDeDias() {
        if (dataInicio == null || dataFim == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoOcorrencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoOcorrencia tipo) {
        this.tipo = tipo;
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

    public SituacaoOcorrencia getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoOcorrencia situacao) {
        this.situacao = situacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }
}
