package com.ibmec.kodie;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoOcorrencia;
import com.ibmec.kodie.model.TipoOcorrencia;
import com.ibmec.kodie.service.OcorrenciaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Telas alteradas nesta versao: edicao de pessoa, exclusao pelo calendario
 * e o formulario de ocorrencia.
 */
@Transactional
class TelasTest extends TesteDeIntegracao {

    @Autowired
    private OcorrenciaService ocorrenciaService;

    @Autowired
    private EntityManager entityManager;

    // ------------------------------------------------------------------
    // Edicao de pessoa
    // ------------------------------------------------------------------

    /** O formulario de edicao da pessoa {id}, preenchido com os campos de "dados". */
    private static MockHttpServletRequestBuilder formularioDeEdicao(Long id, Pessoa dados) {
        MockHttpServletRequestBuilder formulario = post("/pessoas/pagina/{id}/editar", id)
                .param("nome", dados.getNome())
                .param("cpf", dados.getCpf())
                .param("email", dados.getEmail())
                .param("cargo", dados.getCargo())
                .param("tipoVinculo", dados.getTipoVinculo().name())
                .param("situacao", dados.getSituacao().name())
                .param("dataEntrada", dados.getDataEntrada().toString());
        if (dados.getDataSaida() != null) {
            formulario.param("dataSaida", dados.getDataSaida().toString());
        }
        return formulario;
    }

    @Test
    void edicaoValidaSalvaEVoltaParaAFicha() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Para Editar", LocalDate.now().minusYears(1)));

        Pessoa dados = copiaDe(pessoa);
        dados.setCargo("Cargo Novo");

        mvc.perform(formularioDeEdicao(pessoa.getId(), dados).with(comoAdmin()))
                .andExpect(redirectedUrl("/pessoas/pagina/" + pessoa.getId()))
                .andExpect(flash().attribute("mensagem", "Cadastro atualizado"));

        assertThat(pessoaService.buscarPorId(pessoa.getId()).orElseThrow().getCargo()).isEqualTo("Cargo Novo");
    }

    @Test
    void erroDeRegraDeNegocioApareceNoCampoCerto() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Para Editar", LocalDate.now().minusYears(1)));

        // Data de saida sem a situacao "Desligado": o erro e da data de saida, nao do CPF.
        Pessoa dados = copiaDe(pessoa);
        dados.setDataSaida(LocalDate.now());

        mvc.perform(formularioDeEdicao(pessoa.getId(), dados).with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("pessoa-editar"))
                .andExpect(model().attributeHasFieldErrors("pessoaEditada", "dataSaida"))
                .andExpect(model().attributeErrorCount("pessoaEditada", 1));
    }

    @Test
    void perfilDeConsultaNaoSalvaEdicao() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Para Editar", LocalDate.now().minusYears(1)));

        Pessoa dados = copiaDe(pessoa);
        dados.setCargo("Cargo Forjado");

        mvc.perform(formularioDeEdicao(pessoa.getId(), dados).with(comoConsulta()))
                .andExpect(status().isForbidden());

        assertThat(pessoaService.buscarPorId(pessoa.getId()).orElseThrow().getCargo()).isEqualTo("Cargo de teste");
    }

    // ------------------------------------------------------------------
    // Exclusao pelo calendario
    // ------------------------------------------------------------------

    private Ocorrencia feriadoEmDezembro() {
        return ocorrenciaService.criar(new Ocorrencia(TipoOcorrencia.FERIADO_TRABALHADO,
                LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 24),
                SituacaoOcorrencia.PLANEJADA, "Feriado de teste", null));
    }

    @Test
    void excluirPeloCalendarioVoltaParaOMesmoMesEFiltro() throws Exception {
        Ocorrencia feriado = feriadoEmDezembro();
        String mesmoMes = "/calendario/pagina?ano=2026&mes=12&tipo=FERIADO_TRABALHADO";

        mvc.perform(post("/ocorrencias/pagina/{id}/excluir", feriado.getId()).with(comoAdmin())
                        .param("ano", "2026").param("mes", "12").param("tipo", "FERIADO_TRABALHADO"))
                .andExpect(redirectedUrl(mesmoMes))
                .andExpect(flash().attribute("mensagem", "Ocorrência removida"));
        assertThat(ocorrenciaService.buscarPorId(feriado.getId())).isEmpty();

        // Clique duplo: a ocorrencia ja nao existe, e a tela avisa em vez de quebrar.
        mvc.perform(post("/ocorrencias/pagina/{id}/excluir", feriado.getId()).with(comoAdmin())
                        .param("ano", "2026").param("mes", "12").param("tipo", "FERIADO_TRABALHADO"))
                .andExpect(redirectedUrl(mesmoMes))
                .andExpect(flash().attribute("erro", "Esta ocorrência já tinha sido removida"));
    }

    @Test
    void calendarioMostraOBotaoDeExcluirSoParaOAdministrador() throws Exception {
        Ocorrencia feriado = feriadoEmDezembro();
        String acaoDeExcluir = "/ocorrencias/pagina/" + feriado.getId() + "/excluir";

        mvc.perform(get("/calendario/pagina").param("ano", "2026").param("mes", "12").with(comoAdmin()))
                .andExpect(content().string(containsString(acaoDeExcluir)))
                .andExpect(content().string(containsString("data-confirmar=\"Excluir esta ocorrência do calendário?\"")));

        mvc.perform(get("/calendario/pagina").param("ano", "2026").param("mes", "12").with(comoConsulta()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(acaoDeExcluir))));
    }

    // ------------------------------------------------------------------
    // Senhas: o erro aparece no campo certo
    // ------------------------------------------------------------------

    @Test
    void senhaLongaDemaisComAcentosApareceNoCampoDaSenha() throws Exception {
        // 40 letras acentuadas = 80 bytes: passa no limite de 72 caracteres do
        // formulario, mas nao no de 72 bytes do BCrypt. Antes, isto virava um
        // erro em ingles exibido no campo do login.
        String senha = "á".repeat(40);

        mvc.perform(post("/usuarios/pagina").with(comoAdmin())
                        .param("login", "teste.senha.longa")
                        .param("nome", "Teste de Senha Longa")
                        .param("perfil", "VISUALIZADOR")
                        .param("senha", senha)
                        .param("confirmacaoSenha", senha))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("novoUsuario", "senha"))
                .andExpect(model().attributeErrorCount("novoUsuario", 1));
    }

    @Test
    void senhaAtualErradaApareceNoCampoDaSenhaAtual() throws Exception {
        mvc.perform(post("/conta/senha").with(comoAdmin())
                        .param("senhaAtual", "nao-e-a-senha")
                        .param("novaSenha", "senha-nova-123")
                        .param("confirmacaoSenha", "senha-nova-123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("senhaForm", "senhaAtual"))
                .andExpect(model().attributeErrorCount("senhaForm", 1));
    }

    // ------------------------------------------------------------------
    // Formulario de ocorrencia com campos forjados
    // ------------------------------------------------------------------

    @Test
    void formularioDeOcorrenciaNaoAlteraOCadastroDaPessoaNemOutraOcorrencia() throws Exception {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Da Ocorrencia", LocalDate.now().minusYears(1)));
        String cpfOriginal = pessoa.getCpf();
        Ocorrencia existente = ocorrenciaService.criar(new Ocorrencia(TipoOcorrencia.FOLGA,
                LocalDate.of(2031, 2, 3), LocalDate.of(2031, 2, 3), SituacaoOcorrencia.PLANEJADA, "original", pessoa));

        mvc.perform(post("/ocorrencias/pagina").with(comoAdmin())
                        .param("pessoa", pessoa.getId().toString())
                        .param("pessoa.cpf", "99999999999")          // forjado
                        .param("pessoa.nome", "Nome Forjado")        // forjado
                        .param("id", existente.getId().toString())   // forjado
                        .param("tipo", "FOLGA")
                        .param("dataInicio", "2031-03-10")
                        .param("dataFim", "2031-03-10")
                        .param("situacao", "PLANEJADA"))
                .andExpect(redirectedUrl("/pessoas/pagina/" + pessoa.getId()));

        // Grava no banco e esquece o que esta na memoria: o que vale e o que foi gravado.
        entityManager.flush();
        entityManager.clear();

        Pessoa depois = pessoaService.buscarPorId(pessoa.getId()).orElseThrow();
        assertThat(depois.getCpf()).isEqualTo(cpfOriginal);
        assertThat(depois.getNome()).isEqualTo("Pessoa Da Ocorrencia");
        assertThat(ocorrenciaService.buscarPorId(existente.getId()).orElseThrow().getObservacao()).isEqualTo("original");
        assertThat(pessoaService.listarOcorrencias(pessoa.getId())).hasSize(2);
    }
}
