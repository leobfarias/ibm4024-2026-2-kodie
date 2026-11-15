package com.ibmec.kodie.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.PortMapper;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Map;

/**
 * Regras de acesso do sistema.
 *
 *   - Telas (navegador): login por formulario, com sessao.
 *   - API REST (curl, Postman): usuario e senha a cada requisicao (HTTP Basic).
 *
 * Os dois perfis consultam tudo. Apenas o administrador escreve: qualquer
 * POST, PUT ou DELETE, alem das telas de cadastro de usuarios, de registro
 * de ocorrencia e de edicao de pessoa, exige o perfil ADMIN. A unica escrita
 * liberada ao perfil de visualizacao e a troca da propria senha.
 *
 * Alem das regras de acesso, esta classe liga as protecoes do navegador
 * (cabecalhos de seguranca, ver cabecalhosDeSeguranca), o HTTPS obrigatorio
 * em producao e o bloqueio por senhas erradas seguidas (TentativasDeLogin).
 */
@Configuration
public class SegurancaConfig {

    /**
     * Politica de seguranca de conteudo (CSP): de onde o navegador aceita
     * carregar cada tipo de recurso nas paginas do sistema.
     *
     * A linha mais importante e script-src 'self': so rodam scripts vindos
     * de arquivos do proprio sistema. Se algum dia um texto cadastrado
     * conseguir injetar um <script> na pagina (XSS), o navegador se recusa a
     * executa-lo. Por isso nenhum template tem JavaScript embutido: os
     * comportamentos ficam em static/js (tema.js e acoes.js).
     *
     * Os estilos aceitam 'unsafe-inline' porque varias telas usam o
     * atributo style="..." para ajustes pontuais de layout; CSS injetado nao
     * executa codigo. A fonte Sora vem do Google Fonts, unica origem externa.
     */
    static final String POLITICA_DE_CONTEUDO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    /** Recursos do navegador que o sistema nunca usa: nenhuma pagina pode pedi-los. */
    static final String POLITICA_DE_PERMISSOES =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    /** Redirecionamento para HTTPS: o destino e sempre a porta padrao (443, que fica fora da URL). */
    private static final PortMapper HTTPS_NA_PORTA_PADRAO = new PortMapper() {
        @Override
        public Integer lookupHttpsPort(Integer portaHttp) {
            return 443;
        }

        @Override
        public Integer lookupHttpPort(Integer portaHttps) {
            return 80;
        }
    };

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

    /**
     * @param exigirHttps ligado em producao (kodie.seguranca.exigir-https). O
     *                    App Service encerra o TLS e avisa a aplicacao pelo
     *                    cabecalho X-Forwarded-Proto se o pedido chegou por
     *                    http ou https; quem chega por http e redirecionado.
     */
    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http,
                                                 SessionRegistry registroDeSessoes,
                                                 @Value("${kodie.seguranca.exigir-https}") boolean exigirHttps) throws Exception {
        if (exigirHttps) {
            http.redirectToHttps(Customizer.withDefaults())
                // O endereco publico do App Service usa sempre a porta padrao do
                // HTTPS (443), qualquer que seja a porta interna da aplicacao. Sem
                // isto, um pedido que chega direto na porta interna (a sonda de
                // inicializacao da plataforma, por exemplo) dava erro 500: o Spring
                // so conhece de fabrica os pares 80->443 e 8080->8443.
                .portMapper(portas -> portas.portMapper(HTTPS_NA_PORTA_PADRAO));
        }

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
                    .requestMatchers(HttpMethod.GET, "/pessoas/pagina/*/editar").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/**").authenticated()
                    .anyRequest().hasRole("ADMIN"))

            .formLogin(login -> login
                    .loginPage("/login")
                    .defaultSuccessUrl("/inicio")
                    .failureHandler(falhaNoLogin())
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

            .headers(SegurancaConfig::cabecalhosDeSeguranca);

        return http.build();
    }

    /**
     * Cabecalhos que instruem o navegador a se proteger. O Spring Security
     * ja envia, por conta propria:
     *   - X-Content-Type-Options: nosniff (o navegador nao "adivinha" o tipo de um arquivo);
     *   - Cache-Control: no-store (paginas com dados pessoais nao ficam no cache);
     *   - Strict-Transport-Security (HSTS), quando o pedido chega por HTTPS:
     *     por um ano, o navegador so volta ao sistema por HTTPS.
     * Aqui entram os demais.
     */
    private static void cabecalhosDeSeguranca(HeadersConfigurer<HttpSecurity> cabecalhos) {
        cabecalhos
                // Nenhum outro site pode exibir o sistema dentro de um frame (clickjacking).
                // O console do H2 usa frames da propria aplicacao, por isso "sameOrigin".
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                // Ao seguir um link para fora, o outro site nao recebe o endereco
                // da pagina de origem (que pode conter o id de uma pessoa).
                .referrerPolicy(politica -> politica.policy(ReferrerPolicy.SAME_ORIGIN))
                .permissionsPolicyHeader(politica -> politica.policy(POLITICA_DE_PERMISSOES))
                // CSP em todas as paginas, menos no console do H2 (so existe em
                // desenvolvimento), que depende de JavaScript embutido para funcionar.
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        new NegatedRequestMatcher(PathPatternRequestMatcher.pathPattern("/h2-console/**")),
                        new StaticHeadersWriter("Content-Security-Policy", POLITICA_DE_CONTEUDO)));
    }

    /**
     * Login recusado: a tela explica o motivo quando o login esta bloqueado
     * por senhas erradas seguidas. Nos demais casos (senha errada, login
     * inexistente, usuario desativado) a mensagem e a mesma, de proposito:
     * dizer "este login nao existe" ajudaria quem tenta adivinhar.
     */
    private static AuthenticationFailureHandler falhaNoLogin() {
        ExceptionMappingAuthenticationFailureHandler falha = new ExceptionMappingAuthenticationFailureHandler();
        falha.setDefaultFailureUrl("/login?erro");
        falha.setExceptionMappings(Map.of(LockedException.class.getName(), "/login?bloqueado"));
        return falha;
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
     *
     * Antes de tudo: quando o proprio navegador informa que o pedido saiu de
     * OUTRO site (Sec-Fetch-Site: cross-site), nenhuma dispensa vale. Nenhum
     * uso legitimo do sistema faz isso, e a regra fecha o caso de um
     * navegador que guardou a senha do HTTP Basic e a reenvia sozinho.
     */
    private static RequestMatcher semRiscoDeCsrf() {
        return requisicao -> {
            if ("cross-site".equals(requisicao.getHeader("Sec-Fetch-Site"))) {
                return false;
            }
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
