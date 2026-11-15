package com.ibmec.kodie;

import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.RegraDeNegocioException;
import com.ibmec.kodie.service.SituacaoDeFerias;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras do cadastro de pessoas que alimentam o controle de ferias.
 *
 * As datas sao sempre relativas a hoje: o teste continua valido no ano
 * que vem, quando as datas fixas do DataLoader ja tiverem outro resultado.
 */
@Transactional
class PessoaEFeriasTest extends TesteDeIntegracao {

    @Autowired
    private FeriasService feriasService;

    private final LocalDate hoje = LocalDate.now();

    @Test
    void corrigirAEntradaParaTrasRecalculaOsPeriodos() {
        // Antes da correcao, isto estourava a restricao uk_periodo_pessoa_inicio
        // e derrubava o inicio, o painel de ferias e os indicadores com erro 500.
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Com Entrada Errada", hoje.minusYears(2).minusDays(10)));
        assertThat(feriasService.situacaoDe(pessoa).periodos()).hasSize(2);

        Pessoa corrigida = copiaDe(pessoa);
        corrigida.setDataEntrada(hoje.minusYears(4).minusDays(10));
        pessoaService.atualizar(pessoa.getId(), corrigida);

        SituacaoDeFerias depois = feriasService.consultarPorPessoaId(pessoa.getId());
        assertThat(depois.periodos()).hasSize(4);
        assertThat(depois.periodos().getFirst().periodo().getDataInicio()).isEqualTo(hoje.minusYears(4).minusDays(10));
        assertThat(feriasService.montarPainel()).isNotEmpty();
    }

    @Test
    void corrigirAEntradaParaFrenteApagaOsPeriodosQueNaoExistemMais() {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Com Entrada Errada", hoje.minusYears(3).minusDays(10)));
        assertThat(feriasService.situacaoDe(pessoa).periodos()).hasSize(3);

        Pessoa corrigida = copiaDe(pessoa);
        corrigida.setDataEntrada(hoje.minusYears(1).minusDays(10));
        pessoaService.atualizar(pessoa.getId(), corrigida);

        SituacaoDeFerias depois = feriasService.consultarPorPessoaId(pessoa.getId());
        assertThat(depois.periodos()).hasSize(1);
        assertThat(depois.saldoTotal()).isEqualTo(30);
    }

    @Test
    void pessoaDesligadaSaiDoPainelMasAFichaGuardaOHistorico() {
        // Entrou ha 3 anos e nunca tirou ferias: o primeiro periodo ja venceu.
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Que Vai Sair", hoje.minusYears(3).minusDays(10)));
        assertThat(estaNoPainel(pessoa)).isTrue();

        Pessoa desligada = copiaDe(pessoa);
        desligada.setSituacao(SituacaoVinculo.DESLIGADO);
        desligada.setDataSaida(hoje.minusDays(1));
        pessoaService.atualizar(pessoa.getId(), desligada);

        assertThat(estaNoPainel(pessoa)).isFalse();
        assertThat(feriasService.consultarPorPessoaId(pessoa.getId()).periodos()).hasSize(3);
    }

    @Test
    void cadastroNovoNuncaSobrescreveUmaPessoaExistente() {
        Pessoa existente = pessoaService.criar(novaPessoaClt("Pessoa Existente", hoje.minusYears(1)));

        Pessoa comIdForjado = novaPessoaClt("Pessoa Intrusa", hoje.minusYears(1));
        comIdForjado.setId(existente.getId());
        Pessoa criada = pessoaService.criar(comIdForjado);

        assertThat(criada.getId()).isNotEqualTo(existente.getId());
        assertThat(pessoaService.buscarPorId(existente.getId()).orElseThrow().getNome()).isEqualTo("Pessoa Existente");
    }

    @Test
    void regraDeNegocioInformaOCampoDoFormulario() {
        Pessoa pessoa = pessoaService.criar(novaPessoaClt("Pessoa Do Teste", hoje.minusYears(1)));

        Pessoa saidaSemDesligamento = copiaDe(pessoa);
        saidaSemDesligamento.setDataSaida(hoje);
        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), saidaSemDesligamento))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("campo").isEqualTo("dataSaida");

        Pessoa cpfRepetido = novaPessoaClt("Outra Pessoa", hoje.minusYears(1));
        cpfRepetido.setCpf(pessoa.getCpf());
        assertThatThrownBy(() -> pessoaService.criar(cpfRepetido))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("campo").isEqualTo("cpf");
    }

    private boolean estaNoPainel(Pessoa pessoa) {
        return feriasService.montarPainel().stream()
                .anyMatch(s -> s.pessoa().getId().equals(pessoa.getId()));
    }
}
