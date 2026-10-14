package com.ibmec.kodie.controller;

import com.ibmec.kodie.config.UsuarioAutenticado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Disponibiliza o usuario logado para todas as telas.
 *
 * Os templates usam "ehAdmin" para esconder formularios e botoes de escrita
 * do perfil de visualizacao. Esconder e so conforto: quem garante a regra e
 * o SegurancaConfig, que recusa a operacao no servidor mesmo que alguem
 * monte a requisicao na mao.
 */
@ControllerAdvice
public class UsuarioLogadoAdvice {

    @ModelAttribute("usuarioLogado")
    public UsuarioAutenticado usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        return null;
    }

    @ModelAttribute("ehAdmin")
    public boolean ehAdmin() {
        UsuarioAutenticado usuario = usuarioLogado();
        return usuario != null && usuario.isAdmin();
    }
}
