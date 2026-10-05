package com.ibmec.kodie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Cadastro central de pessoas - nucleo do sistema (secao 2.1 do documento de decisoes).
 *
 * Os dados de vinculo (tipo, datas e situacao) estao nesta mesma entidade:
 * no MVP uma pessoa possui um unico vinculo vigente.
 */
@Entity
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome e obrigatorio")
    @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank(message = "O CPF e obrigatorio")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 digitos, sem pontos ou tracos")
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @NotBlank(message = "O e-mail e obrigatorio")
    @Email(message = "Informe um e-mail valido")
    @Size(max = 120, message = "O e-mail deve ter no maximo 120 caracteres")
    @Column(nullable = false, length = 120)
    private String email;

    @NotBlank(message = "O cargo e obrigatorio")
    @Size(max = 80, message = "O cargo deve ter no maximo 80 caracteres")
    @Column(nullable = false, length = 80)
    private String cargo;

    @NotNull(message = "O tipo de vinculo e obrigatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVinculo tipoVinculo;

    @NotNull(message = "A situacao e obrigatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoVinculo situacao = SituacaoVinculo.ATIVO;

    @NotNull(message = "A data de entrada e obrigatoria")
    @PastOrPresent(message = "A data de entrada nao pode estar no futuro")
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataEntrada;

    /** Preenchida apenas quando a pessoa e desligada. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataSaida;

    public Pessoa() {
    }

    public Pessoa(String nome, String cpf, String email, String cargo,
                  TipoVinculo tipoVinculo, LocalDate dataEntrada) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.cargo = cargo;
        this.tipoVinculo = tipoVinculo;
        this.dataEntrada = dataEntrada;
        this.situacao = SituacaoVinculo.ATIVO;
    }

    /** Usada pelo painel de ferias: so CLT acumula periodo aquisitivo (secao 2.4). */
    public boolean possuiRegraDeFerias() {
        return tipoVinculo != null && tipoVinculo.temRegraDeFerias();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public TipoVinculo getTipoVinculo() {
        return tipoVinculo;
    }

    public void setTipoVinculo(TipoVinculo tipoVinculo) {
        this.tipoVinculo = tipoVinculo;
    }

    public SituacaoVinculo getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoVinculo situacao) {
        this.situacao = situacao;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDate dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDate dataSaida) {
        this.dataSaida = dataSaida;
    }
}
