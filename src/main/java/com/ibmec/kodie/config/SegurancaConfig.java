package com.ibmec.kodie.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Regras de acesso do sistema.
 *
 *   - Telas (navegador): login por formulario, com sessao.
 *   - API REST (curl, Postman): usuario e senha a cada requisicao (HTTP Basic).
 *
 * Os dois perfis consultam tudo. Apenas o administrador escreve: qualquer
 * POST, PUT ou DELETE, alem das telas de cadastro de usuarios e de registro
 * de ocorrencia, exige o perfil ADMIN. A unica escrita liberada ao perfil de
 * visualizacao e a troca da propria senha.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Registro das sessoes abertas, para que possam ser encerradas pelo administrador. */
    @Bean
    public SessionRegistry registroDeSessoes() {
        return new SessionRegistryImpl();
    }

    /** Avisa o registro quando uma sessao termina, para ele nao acumular sessoes mortas. */
    @Bean
    public HttpSessionEventPublisher publicadorDeEventosDeSessao() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, SessionRegistry registroDeSessoes) throws Exception {
        http
            .authorizeHttpRequests(acesso -> acesso
                    .requestMatchers("/login", "/acesso-negado", "/error", "/css/**", "/js/**", "/img/**").permitAll()
                    // Pedidos que os navegadores fazem sozinhos. Exigir login neles fazia o
                    // navegador abrir a janela nativa de autenticacao.
                    .requestMatchers("/favicon.ico", "/apple-touch-icon*.png", "/robots.txt", "/.well-known/**").permitAll()
                    .requestMatchers("/h2-console/**").hasRole("ADMIN")
                    .requestMatchers("/usuarios/**").hasRole("ADMIN")
                    .requestMatchers("/backup", "/backup/**").hasRole("ADMIN")
                    .requestMatchers("/conta/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/ocorrencias/pagina/nova").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/**").authenticated()
                    .anyRequest().hasRole("ADMIN"))

            .formLogin(login -> login
                    .loginPage("/login")
                    .defaultSuccessUrl("/inicio")
                    .failureUrl("/login?erro")
                    .permitAll())

            .logout(saida -> saida
                    .logoutSuccessUrl("/login?saiu")
                    .permitAll())

            .httpBasic(basic -> basic.authenticationEntryPoint(RespostasDeSegurancaApi.naoAutenticado()))

            // Sem limite de sessoes por usuario; o registro serve para encerra-las.
            .sessionManagement(sessoes -> sessoes.sessionConcurrency(concorrencia -> concorrencia
                    .maximumSessions(-1)
                    .sessionRegistry(registroDeSessoes)
                    .expiredUrl("/login?encerrada")))

            // Depois do login, o usuario volta para a pagina que tentou abrir. So paginas
            // entram nessa memoria: um icone pedido em segundo plano fazia o login
            // terminar no endereco do icone em vez de na pagina inicial.
            .requestCache(memoria -> memoria.requestCache(paginasPedidas()))

            .exceptionHandling(erros -> erros
                    .authenticationEntryPoint(semAutenticacao())
                    .accessDeniedHandler(semPermissao()))

            .csrf(csrf -> csrf
                    .ignoringRequestMatchers(semRiscoDeCsrf())
                    .ignoringRequestMatchers("/h2-console/**"))

            // O console do H2 e exibido dentro de um frame da propria aplicacao.
            .headers(cabecalhos -> cabecalhos.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * Quem chega sem login:
     *   - abrindo uma pagina: vai para a tela de login;
     *   - pedido do navegador em segundo plano (imagem, script): 401 sem desafio,
     *     para nao abrir a janela nativa de autenticacao;
     *   - cliente de API (curl, Postman): 401 com o desafio Basic.
     */
    private AuthenticationEntryPoint semAutenticacao() {
        AuthenticationEntryPoint telaDeLogin = new LoginUrlAuthenticationEntryPoint("/login");
        AuthenticationEntryPoint recursoDoNavegador = RespostasDeSegurancaApi.naoAutenticado(false);
        AuthenticationEntryPoint clienteDeApi = RespostasDeSegurancaApi.naoAutenticado(true);
        return (requisicao, resposta, excecao) -> {
            if (ehNavegador(requisicao)) {
                telaDeLogin.commence(requisicao, resposta, excecao);
            } else if (ehPedidoDeNavegador(requisicao)) {
                recursoDoNavegador.commence(requisicao, resposta, excecao);
            } else {
                clienteDeApi.commence(requisicao, resposta, excecao);
            }
        };
    }

    /** Guarda, para depois do login, apenas a abertura de paginas. */
    private static HttpSessionRequestCache paginasPedidas() {
        HttpSessionRequestCache memoria = new HttpSessionRequestCache();
        memoria.setRequestMatcher(requisicao -> "GET".equals(requisicao.getMethod()) && ehNavegador(requisicao));
        return memoria;
    }

    /**
     * Navegador ve a pagina de acesso negado; cliente de API recebe 403 em JSON.
     *
     * A falha de CSRF e tratada a parte, porque nao significa falta de
     * permissao: no navegador, quase sempre e uma sessao que expirou com o
     * formulario aberto; na API, e uma requisicao enviada sem credenciais.
     * Em nenhum dos dois casos a mensagem "o seu perfil permite apenas
     * consulta" seria verdadeira.
     */
    private AccessDeniedHandler semPermissao() {
        AccessDeniedHandlerImpl pagina = new AccessDeniedHandlerImpl();
        pagina.setErrorPage("/acesso-negado");
        AccessDeniedHandler api = RespostasDeSegurancaApi.acessoNegado();
        AuthenticationEntryPoint apiSemCredencial = RespostasDeSegurancaApi.naoAutenticado();

        return (requisicao, resposta, excecao) -> {
            boolean navegador = ehNavegador(requisicao);

            if (excecao instanceof CsrfException) {
                if (navegador) {
                    resposta.sendRedirect(requisicao.getContextPath() + "/login?expirou");
                } else {
                    apiSemCredencial.commence(requisicao, resposta,
                            new InsufficientAuthenticationException(excecao.getMessage()));
                }
                return;
            }

            (navegador ? pagina : api).handle(requisicao, resposta, excecao);
        };
    }

    /** Abertura de uma pagina: o navegador pede HTML. */
    private static boolean ehNavegador(HttpServletRequest requisicao) {
        if ("navigate".equals(requisicao.getHeader("Sec-Fetch-Mode"))) {
            return true;
        }
        String aceita = requisicao.getHeader("Accept");
        return aceita != null && aceita.contains("text/html");
    }

    /**
     * Qualquer pedido feito por um navegador moderno, inclusive em segundo
     * plano: todos trazem os cabecalhos Sec-Fetch-*. Clientes de API como o
     * curl e o Postman nao os enviam.
     */
    private static boolean ehPedidoDeNavegador(HttpServletRequest requisicao) {
        return requisicao.getHeader("Sec-Fetch-Dest") != null || requisicao.getHeader("Sec-Fetch-Mode") != null;
    }

    /**
     * Requisicoes que um site malicioso nao consegue forjar a partir do
     * navegador da vitima, e que por isso dispensam o token CSRF:
     *
     *   - as que trazem o cabecalho Authorization (o navegador nao o envia sozinho);
     *   - PUT, DELETE e PATCH, que entre origens diferentes exigem permissao CORS,
     *     e o sistema nao concede nenhuma;
     *   - corpo application/json, pelo mesmo motivo.
     *
     * Sobram os formularios HTML, que continuam exigindo o token - o Thymeleaf
     * o inclui automaticamente em todo th:action.
     */
    private static RequestMatcher semRiscoDeCsrf() {
        return requisicao -> {
            if (requisicao.getHeader("Authorization") != null) {
                return true;
            }
            String metodo = requisicao.getMethod();
            if ("PUT".equals(metodo) || "DELETE".equals(metodo) || "PATCH".equals(metodo)) {
                return true;
            }
            String tipo = requisicao.getContentType();
            return tipo != null && tipo.startsWith("application/json");
        };
    }
}
