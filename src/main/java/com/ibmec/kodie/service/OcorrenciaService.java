package com.ibmec.kodie.service;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OcorrenciaService {

    private final OcorrenciaRepository repository;
    private final PessoaRepository pessoaRepository;

    public OcorrenciaService(OcorrenciaRepository repository, PessoaRepository pessoaRepository) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
    }

    public List<Ocorrencia> listarTodos() {
        return repository.findAll();
    }

    public Optional<Ocorrencia> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Ocorrencia criar(Ocorrencia ocorrencia) {
        validarDatas(ocorrencia);
        resolverPessoa(ocorrencia);
        validarSobreposicao(ocorrencia, null);
        return repository.save(ocorrencia);
    }

    public Ocorrencia atualizar(Long id, Ocorrencia dados) {
        Ocorrencia existente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrencia nao encontrada: " + id));

        validarDatas(dados);
        resolverPessoa(dados);
        validarSobreposicao(dados, id);

        existente.setTipo(dados.getTipo());
        existente.setDataInicio(dados.getDataInicio());
        existente.setDataFim(dados.getDataFim());
        existente.setSituacao(dados.getSituacao());
        existente.setObservacao(dados.getObservacao());
        existente.setPessoa(dados.getPessoa());

        return repository.save(existente);
    }

    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Ocorrencia nao encontrada: " + id);
        }
        repository.deleteById(id);
    }

    private void validarDatas(Ocorrencia ocorrencia) {
        if (ocorrencia.getDataInicio() != null && ocorrencia.getDataFim() != null
                && ocorrencia.getDataFim().isBefore(ocorrencia.getDataInicio())) {
            throw new IllegalArgumentException("A data de fim nao pode ser anterior a data de inicio");
        }
    }

    /**
     * O JSON e o formulario enviam apenas o id da pessoa. Aqui ele e trocado
     * pela entidade real, o que tambem prova que a pessoa existe.
     */
    private void resolverPessoa(Ocorrencia ocorrencia) {
        Pessoa pessoa = ocorrencia.getPessoa();
        if (pessoa == null || pessoa.getId() == null) {
            ocorrencia.setPessoa(null);
            return;
        }

        Pessoa encontrada = pessoaRepository.findById(pessoa.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa nao encontrada: " + pessoa.getId()));

        if (encontrada.getSituacao() == SituacaoVinculo.DESLIGADO) {
            throw new IllegalArgumentException(
                    "Nao e possivel registrar ocorrencia para uma pessoa desligada");
        }

        ocorrencia.setPessoa(encontrada);
    }

    /**
     * Regra de negocio com consulta ao banco: a mesma pessoa nao pode ter
     * duas ocorrencias ativas no mesmo intervalo de datas.
     */
    private void validarSobreposicao(Ocorrencia ocorrencia, Long idIgnorado) {
        if (ocorrencia.getPessoa() == null
                || ocorrencia.getSituacao() == SituacaoOcorrencia.CANCELADA) {
            return;
        }

        Long pessoaId = ocorrencia.getPessoa().getId();
        boolean conflita = (idIgnorado == null)
                ? repository.existsByPessoaIdAndSituacaoNotAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        pessoaId, SituacaoOcorrencia.CANCELADA,
                        ocorrencia.getDataFim(), ocorrencia.getDataInicio())
                : repository.existsByPessoaIdAndIdNotAndSituacaoNotAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        pessoaId, idIgnorado, SituacaoOcorrencia.CANCELADA,
                        ocorrencia.getDataFim(), ocorrencia.getDataInicio());

        if (conflita) {
            throw new IllegalArgumentException(
                    "Esta pessoa ja possui uma ocorrencia registrada neste periodo");
        }
    }
}
