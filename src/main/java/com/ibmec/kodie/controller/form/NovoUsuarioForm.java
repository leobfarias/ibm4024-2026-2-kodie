package com.ibmec.kodie.controller.form;

import com.ibmec.kodie.model.PerfilAcesso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulario de novo usuario.
 *
 * Existe separado da entidade Usuario porque o formulario recebe a senha em
 * texto, e a entidade so guarda o hash: ligar o formulario direto na
 * entidade permitiria a alguem enviar um campo senhaHash forjado.
 */
public class NovoUsuarioForm {

    @NotBlank(message = "O login e obrigatorio")
    @Size(min = 3, max = 40, message = "O login deve ter entre 3 e 40 caracteres")
    @Pattern(regexp = "[a-z0-9._-]*",
             message = "O login aceita apenas letras minusculas, numeros, ponto, hifen e sublinhado")
    private String login;

    @NotBlank(message = "O nome e obrigatorio")
    @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
    private String nome;

    @NotNull(message = "O perfil de acesso e obrigatorio")
    private PerfilAcesso perfil = PerfilAcesso.VISUALIZADOR;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotBlank(message = "Repita a senha")
    private String confirmacaoSenha;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public PerfilAcesso getPerfil() { return perfil; }
    public void setPerfil(PerfilAcesso perfil) { this.perfil = perfil; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}
