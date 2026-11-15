package com.ibmec.kodie;

import com.ibmec.kodie.config.TentativasDeLogin;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regras de acesso, protecao contra CSRF, bloqueio de senhas erradas e
 * cabecalhos de seguranca - tudo pelo caminho real das requisicoes.
 */
class SegurancaTest extends TesteDeIntegracao {

    @Autowired
    private UsuarioService usuarioService;

    // ------------------------------------------------------------------
    // Quem pode o que
    // ------------------------------------------------------------------

    @Test
    void quemNaoEntrouVaiParaATelaDeLogin() throws Exception {
        mvc.perform(get("/pessoas/pagina").accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiSemSenhaRecebe401() throws Exception {
        mvc.perform(get("/pessoas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilDeConsultaNaoAbreAEdicaoDePessoa() throws Exception {
        Long id = pessoaService.listarTodos().getFirst().getId();

        mvc.perform(get("/pessoas/pagina/{id}/editar", id).with(comoConsulta()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pessoas/pagina/{id}/editar", id).with(comoAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void perfilDeConsultaNaoExcluiOcorrencia() throws Exception {
        mvc.perform(post("/ocorrencias/pagina/{id}/excluir", 1).with(comoConsulta()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // CSRF
    // ------------------------------------------------------------------

    @Test
    @Transactional
    void formularioSemTokenCsrfERecusado() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Do Teste Csrf", LocalDate.now().minusYears(1)));

        mvc.perform(post("/pessoas/pagina/{id}/excluir", pessoa.getId())
                        .with(user("admin").roles("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/login?expirou"));

        assertThat(pessoaService.buscarPorId(pessoa.getId())).isPresent();
    }

    @Test
    @Transactional
    void formularioComTokenCsrfFunciona() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Do Teste Csrf", LocalDate.now().minusYears(1)));

        mvc.perform(post("/pessoas/pagina/{id}/excluir", pessoa.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(redirectedUrl("/pessoas/pagina"));

        assertThat(pessoaService.buscarPorId(pessoa.getId())).isEmpty();
    }

    @Test
    @Transactional
    void pedidoVindoDeOutroSiteNuncaDispensaOToken() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Do Teste Csrf", LocalDate.now().minusYears(1)));

        // Mesmo com a senha no cabecalho, um formulario disparado de outro site e recusado.
        mvc.perform(post("/pessoas/pagina/{id}/excluir", pessoa.getId())
                        .with(comoAdmin())
                        .header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isUnauthorized());

        assertThat(pessoaService.buscarPorId(pessoa.getId())).isPresent();
    }

    @Test
    @Transactional
    void apiComSenhaContinuaSemPrecisarDoToken() throws Exception {
        String json = """
                {"nome":"Pessoa Pela Api","cpf":"98765432100","email":"api@exemplo.com",
                 "cargo":"Teste","tipoVinculo":"PJ","dataEntrada":"2025-01-10"}
                """;
        mvc.perform(post("/pessoas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Bloqueio por senhas erradas seguidas
    // ------------------------------------------------------------------

    @Test
    void dezSenhasErradasBloqueiamOLoginInclusiveComASenhaCerta() throws Exception {
        usuarioService.criar("teste.bloqueio", "Teste de Bloqueio", "senha-certa-123", PerfilAcesso.VISUALIZADOR);

        for (int i = 0; i < TentativasDeLogin.LIMITE; i++) {
            mvc.perform(formLogin().user("teste.bloqueio").password("senha-errada"))
                    .andExpect(redirectedUrl("/login?erro"));
        }

        mvc.perform(formLogin().user("teste.bloqueio").password("senha-certa-123"))
                .andExpect(redirectedUrl("/login?bloqueado"));

        // A API passa pelo mesmo bloqueio.
        mvc.perform(get("/pessoas").with(httpBasic("teste.bloqueio", "senha-certa-123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginQueNaoExisteRecebeAMesmaRespostaQueUmQueExiste() throws Exception {
        for (int i = 0; i < TentativasDeLogin.LIMITE; i++) {
            mvc.perform(formLogin().user("ninguem.com.este.login").password("qualquer"))
                    .andExpect(redirectedUrl("/login?erro"));
        }
        mvc.perform(formLogin().user("ninguem.com.este.login").password("qualquer"))
                .andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void entrarComASenhaCertaZeraAContagem() throws Exception {
        usuarioService.criar("teste.contagem", "Teste de Contagem", "senha-certa-123", PerfilAcesso.VISUALIZADOR);

        for (int rodada = 0; rodada < 2; rodada++) {
            for (int i = 0; i < TentativasDeLogin.LIMITE - 1; i++) {
                mvc.perform(formLogin().user("teste.contagem").password("senha-errada"))
                        .andExpect(redirectedUrl("/login?erro"));
            }
            mvc.perform(formLogin().user("teste.contagem").password("senha-certa-123"))
                    .andExpect(redirectedUrl("/inicio"));
        }
    }

    // ------------------------------------------------------------------
    // Protecoes do navegador
    // ------------------------------------------------------------------

    @Test
    void paginasTrazemOsCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'self'")))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void consoleDoH2FicaForaDaCsp() throws Exception {
        mvc.perform(get("/h2-console/").with(comoAdmin()))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    /**
     * A CSP so deixa rodar scripts de arquivos do sistema. Um <script> sem
     * src, um onclick="..." ou um href="javascript:..." em qualquer tela
     * seria bloqueado pelo navegador e a funcao dele pararia de funcionar
     * em silencio. Este teste pega isso antes de chegar a cliente.
     */
    @Test
    void nenhumaTelaTemJavascriptEmbutido() throws Exception {
        Long id = pessoaService.listarTodos().getFirst().getId();
        List<String> telas = List.of(
                "/inicio", "/indicadores/pagina", "/calendario/pagina?ano=2026&mes=11",
                "/pessoas/pagina", "/pessoas/pagina/" + id, "/pessoas/pagina/" + id + "/editar",
                "/ferias/painel", "/ocorrencias/pagina/nova", "/usuarios/pagina",
                "/backup/pagina", "/conta/senha");

        for (String tela : telas) {
            String html = mvc.perform(get(tela).with(comoAdmin()).accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(javascriptEmbutido(html)).as("JavaScript embutido em %s", tela).isEmpty();
        }

        String login = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        assertThat(javascriptEmbutido(login)).as("JavaScript embutido em /login").isEmpty();
    }

    /** Devolve o primeiro trecho de JavaScript embutido encontrado, ou "" se nao houver. */
    private static String javascriptEmbutido(String html) {
        Matcher achado = Pattern.compile(
                "<script(?![^>]*\\ssrc=)[^>]*>|\\son[a-z]+\\s*=|javascript:", Pattern.CASE_INSENSITIVE)
                .matcher(html);
        return achado.find() ? achado.group() : "";
    }
}
