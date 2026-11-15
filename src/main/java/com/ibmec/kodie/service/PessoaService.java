package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.model.TipoVinculo;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PeriodoAquisitivoRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PessoaService {

    private final PessoaRepository repository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final PeriodoAquisitivoRepository periodoRepository;

    public PessoaService(PessoaRepository repository,
                         OcorrenciaRepository ocorrenciaRepository,
                         PeriodoAquisitivoRepository periodoRepository) {
        this.repository = repository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.periodoRepository = periodoRepository;
    }

    public List<Pessoa> listarTodos() {
        return repository.findAll();
    }

    public Optional<Pessoa> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** Usada pela pagina: cada pessoa acompanhada do seu total de ocorrencias. */
    public List<PessoaResumo> listarResumo() {
        return listarResumo(null, null, null);
    }

    /**
     * Mesma listagem, restringida pelos filtros da tela.
     *
     * A busca vira um padrao LIKE ja pronto - "%ana%" -, ou apenas "%"
     * quando esta em branco, o que casa com qualquer nome.
     */
    public List<PessoaResumo> listarResumo(String busca, TipoVinculo tipoVinculo, SituacaoVinculo situacao) {
        String padrao = (busca == null || busca.isBlank())
                ? "%"
                : "%" + busca.trim().toLowerCase() + "%";
        return repository.buscarComFiltros(padrao, tipoVinculo, situacao).stream()
                .map(p -> new PessoaResumo(p, ocorrenciaRepository.countByPessoaId(p.getId())))
                .toList();
    }

    public long contarTodas() {
        return repository.count();
    }

    public long contarAtivas() {
        return repository.countBySituacao(SituacaoVinculo.ATIVO);
    }

    /**
     * Ocorrencias de uma pessoa.
     *
     * A verificacao de existencia e indispensavel: sem ela, um id inexistente
     * devolveria lista vazia com 200. Nao achar nada e diferente de o pai nao existir.
     */
    public List<Ocorrencia> listarOcorrencias(Long pessoaId) {
        if (!repository.existsById(pessoaId)) {
            throw new RecursoNaoEncontradoException("Pessoa não encontrada: " + pessoaId);
        }
        return ocorrenciaRepository.findByPessoaIdOrderByDataInicioDesc(pessoaId);
    }

    /**
     * Regra de negocio com consulta ao banco: o cadastro central nao admite
     * duas pessoas com o mesmo CPF (secao 2.1 do documento de decisoes).
     */
    public Pessoa criar(Pessoa pessoa) {
        // Cadastro novo nunca traz id: quem o define e o banco. Sem esta linha,
        // um JSON com "id": 1 faria o save() sobrescrever a pessoa 1.
        pessoa.setId(null);

        if (repository.existsByCpf(pessoa.getCpf())) {
            throw new RegraDeNegocioException("cpf",
                    "Já existe uma pessoa cadastrada com o CPF " + pessoa.getCpf());
        }
        aplicarRegrasDeDesligamento(pessoa);
        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa atualizar(Long id, Pessoa dados) {
        Pessoa existente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));

        if (repository.existsByCpfAndIdNot(dados.getCpf(), id)) {
            throw new RegraDeNegocioException("cpf",
                    "Já existe outra pessoa cadastrada com o CPF " + dados.getCpf());
        }

        aplicarRegrasDeDesligamento(dados);

        // Os periodos aquisitivos sao montados a partir da data de entrada, da
        // data de saida e do vinculo. Se um deles mudou, os periodos gravados
        // ficaram errados - e o FeriasService so acrescenta periodos, nunca
        // corrige os que ja existem. Entao apagamos: ele os recria certos na
        // proxima consulta. Nenhuma informacao se perde, porque o saldo vem
        // das ocorrencias de ferias, e nao dos periodos.
        boolean mudouABaseDasFerias =
                !Objects.equals(existente.getDataEntrada(), dados.getDataEntrada())
                || !Objects.equals(existente.getDataSaida(), dados.getDataSaida())
                || existente.getTipoVinculo() != dados.getTipoVinculo();
        if (mudouABaseDasFerias) {
            periodoRepository.deleteByPessoaId(id);
        }

        existente.setNome(dados.getNome());
        existente.setCpf(dados.getCpf());
        existente.setEmail(dados.getEmail());
        existente.setCargo(dados.getCargo());
        existente.setTipoVinculo(dados.getTipoVinculo());
        existente.setSituacao(dados.getSituacao());
        existente.setDataEntrada(dados.getDataEntrada());
        existente.setDataSaida(dados.getDataSaida());

        return repository.save(existente);
    }

    /**
     * Remove a pessoa e, junto, as ocorrencias dela.
     *
     * Sem apagar os filhos primeiro, o banco recusaria a operacao por causa da
     * chave estrangeira. Ocorrencias sem pessoa vinculada nao sao afetadas.
     */
    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Pessoa não encontrada: " + id);
        }
        ocorrenciaRepository.deleteAll(ocorrenciaRepository.findByPessoaIdOrderByDataInicioDesc(id));
        periodoRepository.deleteByPessoaId(id);
        repository.deleteById(id);
    }

    /**
     * Coerencia entre situacao e data de saida: sao campos separados que
     * precisam contar a mesma historia.
     */
    private void aplicarRegrasDeDesligamento(Pessoa pessoa) {
        boolean desligada = pessoa.getSituacao() == SituacaoVinculo.DESLIGADO;

        if (desligada && pessoa.getDataSaida() == null) {
            throw new RegraDeNegocioException("dataSaida", "Informe a data de saída para uma pessoa desligada");
        }
        if (!desligada && pessoa.getDataSaida() != null) {
            throw new RegraDeNegocioException("dataSaida", "Só é permitido informar data de saída quando a situação for Desligado");
        }
        if (pessoa.getDataSaida() != null && pessoa.getDataEntrada() != null
                && pessoa.getDataSaida().isBefore(pessoa.getDataEntrada())) {
            throw new RegraDeNegocioException("dataSaida", "A data de saída não pode ser anterior à data de entrada");
        }
    }
}
