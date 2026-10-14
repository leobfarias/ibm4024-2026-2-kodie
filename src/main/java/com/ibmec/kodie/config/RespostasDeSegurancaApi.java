package com.ibmec.kodie.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas de seguranca para a API REST, no mesmo formato JSON e em
 * portugues que o ManipuladorDeErros usa para os demais erros.
 *
 * O navegador recebe outra coisa: redirecionamento para a tela de login
 * (401) ou a pagina de acesso negado (403).
 */
public final class RespostasDeSegurancaApi {

    private RespostasDeSegurancaApi() {
    }

    /** 401: a requisicao nao trouxe credenciais validas. */
    public static AuthenticationEntryPoint naoAutenticado() {
        return (requisicao, resposta, excecao) -> {
            resposta.setHeader("WWW-Authenticate", "Basic realm=\"KODIE\"");
            escrever(resposta, HttpServletResponse.SC_UNAUTHORIZED,
                    "Autenticacao necessaria: informe usuario e senha");
        };
    }

    /** 403: o usuario e conhecido, mas o perfil dele nao permite a operacao. */
    public static AccessDeniedHandler acessoNegado() {
        return (requisicao, resposta, excecao) ->
                escrever(resposta, HttpServletResponse.SC_FORBIDDEN,
                        "Acesso negado: o seu perfil permite apenas consulta");
    }

    private static void escrever(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
