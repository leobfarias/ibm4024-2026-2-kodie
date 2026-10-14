package com.ibmec.kodie.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Tela de login e pagina de acesso negado.
 *
 * O processamento do login (POST /login) e da saida (POST /logout) e feito
 * pelo proprio Spring Security; aqui ficam apenas as telas.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        boolean jaLogado = autenticacao != null
                && autenticacao.isAuthenticated()
                && !(autenticacao instanceof AnonymousAuthenticationToken);
        return jaLogado ? "redirect:/pessoas/pagina" : "login";
    }

    /**
     * Sem metodo HTTP fixo: quando um usuario de consulta tenta um POST, o
     * Spring Security encaminha a propria requisicao POST para ca.
     */
    @RequestMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
