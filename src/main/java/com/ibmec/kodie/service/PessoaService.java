package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PessoaService {

    private final PessoaRepository repository;
    private final OcorrenciaRepository ocorrenciaRepository;

    public PessoaService(PessoaRepository repository, OcorrenciaRepository ocorrenciaRepository) {
        this.repository = repository;
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    public List<Pessoa> listarTodos() {
        return repository.findAll();
    }

    public Optional<Pessoa> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** Usada pela pagina: cada pessoa acompanhada do seu total de ocorrencias. */
    public List<PessoaResumo> listarResumo() {
        return repository.findAll().stream()
                .map(p -> new PessoaResumo(p, ocorrenciaRepository.countByPessoaId(p.getId())))
                .toList();
    }

    /**
     * Ocorrencias de uma pessoa.
     *
     * A verificacao de existencia e indispensavel: sem ela, um id inexistente
     * devolveria lista vazia com 200. Nao achar nada e diferente de o pai nao existir.
     */
    public List<Ocorrencia> listarOcorrencias(Long pessoaId) {
        if (!repository.existsById(pessoaId)) {
            throw new RecursoNaoEncontradoException("Pessoa nao encontrada: " + pessoaId);
        }
        return ocorrenciaRepository.findByPessoaIdOrderByDataInicioDesc(pessoaId);
    }

    /**
     * Regra de negocio com consulta ao banco: o cadastro central nao admite
     * duas pessoas com o mesmo CPF (secao 2.1 do documento de decisoes).
     */
    public Pessoa criar(Pessoa pessoa) {
        if (repository.existsByCpf(pessoa.getCpf())) {
            throw new IllegalArgumentException(
                    "Ja existe uma pessoa cadastrada com o CPF " + pessoa.getCpf());
        }
        aplicarRegrasDeDesligamento(pessoa);
        return repository.save(pessoa);
    }

    public Pessoa atualizar(Long id, Pessoa dados) {
        Pessoa existente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa nao encontrada: " + id));

        if (repository.existsByCpfAndIdNot(dados.getCpf(), id)) {
            throw new IllegalArgumentException(
                    "Ja existe outra pessoa cadastrada com o CPF " + dados.getCpf());
        }

        aplicarRegrasDeDesligamento(dados);

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
            throw new RecursoNaoEncontradoException("Pessoa nao encontrada: " + id);
        }
        ocorrenciaRepository.deleteAll(ocorrenciaRepository.findByPessoaIdOrderByDataInicioDesc(id));
        repository.deleteById(id);
    }

    /**
     * Coerencia entre situacao e data de saida: sao campos separados que
     * precisam contar a mesma historia.
     */
    private void aplicarRegrasDeDesligamento(Pessoa pessoa) {
        boolean desligada = pessoa.getSituacao() == SituacaoVinculo.DESLIGADO;

        if (desligada && pessoa.getDataSaida() == null) {
            throw new IllegalArgumentException("Informe a data de saida para uma pessoa desligada");
        }
        if (!desligada && pessoa.getDataSaida() != null) {
            throw new IllegalArgumentException("So e permitido informar data de saida quando a situacao for Desligado");
        }
        if (pessoa.getDataSaida() != null && pessoa.getDataEntrada() != null
                && pessoa.getDataSaida().isBefore(pessoa.getDataEntrada())) {
            throw new IllegalArgumentException("A data de saida nao pode ser anterior a data de entrada");
        }
    }
}
