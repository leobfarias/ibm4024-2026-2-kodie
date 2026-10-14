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

    /** 401 para clientes de API: com o desafio Basic, que informa como se autenticar. */
    public static AuthenticationEntryPoint naoAutenticado() {
        return naoAutenticado(true);
    }

    /**
     * 401: a requisicao nao trouxe credenciais validas.
     *
     * @param comDesafioBasic falso para pedidos feitos pelo navegador em segundo
     *                        plano (icones, imagens): o cabecalho WWW-Authenticate
     *                        faria o navegador abrir a janela nativa de login, por
     *                        cima da tela de login do proprio sistema.
     */
    public static AuthenticationEntryPoint naoAutenticado(boolean comDesafioBasic) {
        return (requisicao, resposta, excecao) -> {
            if (comDesafioBasic) {
                resposta.setHeader("WWW-Authenticate", "Basic realm=\"KODIE\"");
            }
            escrever(resposta, HttpServletResponse.SC_UNAUTHORIZED,
                    "Autenticação necessária: informe usuário e senha");
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
