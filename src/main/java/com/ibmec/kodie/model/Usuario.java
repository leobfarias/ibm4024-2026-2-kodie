package com.ibmec.kodie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Usuario do sistema - quem opera o cadastro, nao uma pessoa cadastrada.
 *
 * A senha nunca e armazenada: guarda-se apenas o hash BCrypt dela. Por isso
 * este campo nao e preenchido por formulario; os formularios recebem a senha
 * em texto e o UsuarioService gera o hash.
 */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O login e obrigatorio")
    @Size(min = 3, max = 40, message = "O login deve ter entre 3 e 40 caracteres")
    @Pattern(regexp = "[a-z0-9._-]+",
             message = "O login aceita apenas letras minusculas, numeros, ponto, hifen e sublinhado")
    @Column(nullable = false, unique = true, length = 40)
    private String login;

    @NotBlank(message = "O nome e obrigatorio")
    @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 100)
    private String senhaHash;

    @NotNull(message = "O perfil de acesso e obrigatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilAcesso perfil;

    /** Usuario inativo nao consegue entrar, mas o registro e preservado. */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Usuario() {
    }

    public Usuario(String login, String nome, String senhaHash, PerfilAcesso perfil) {
        this.login = login;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return perfil == PerfilAcesso.ADMIN;
    }

    @Transient
    public String getIniciais() {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + segunda).toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public PerfilAcesso getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilAcesso perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
