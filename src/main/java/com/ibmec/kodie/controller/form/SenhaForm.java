package com.ibmec.kodie.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha. A senha atual so e exigida quando o proprio usuario troca
 * a sua; quando o administrador redefine a de outra pessoa, ela fica vazia.
 */
public class SenhaForm {

    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String novaSenha;

    @NotBlank(message = "Repita a nova senha")
    private String confirmacaoSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }

    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}
