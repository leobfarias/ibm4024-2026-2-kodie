package com.ibmec.kodie.service;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PeriodoAquisitivoRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controle de ferias conforme a secao 2.3 do documento de decisoes.
 *
 * Duas decisoes de modelagem sustentam esta classe:
 *
 * 1. O saldo nunca e armazenado. Ele e calculado a cada consulta, a partir
 *    dos periodos aquisitivos e das ocorrencias do tipo FERIAS. Nenhum dado
 *    precisa ser sobrescrito e o historico permanece intacto.
 *
 * 2. Nao existe rotina periodica. Os periodos faltantes sao criados sob
 *    demanda, de forma idempotente, quando a situacao da pessoa e consultada.
 */
@Service
public class FeriasService {

    private final PeriodoAquisitivoRepository periodoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final PessoaRepository pessoaRepository;

    /** Antecedencia do alerta de vencimento, definida com a cliente. */
    private final int diasDeAlerta;

    public FeriasService(PeriodoAquisitivoRepository periodoRepository,
                         OcorrenciaRepository ocorrenciaRepository,
                         PessoaRepository pessoaRepository,
                         @Value("${kodie.ferias.dias-alerta-vencimento:90}") int diasDeAlerta) {
        this.periodoRepository = periodoRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.pessoaRepository = pessoaRepository;
        this.diasDeAlerta = diasDeAlerta;
    }

    public int getDiasDeAlerta() {
        return diasDeAlerta;
    }

    /** Painel de ferias: a situacao de todas as pessoas do cadastro. */
    @Transactional
    public List<SituacaoDeFerias> montarPainel() {
        return pessoaRepository.findAll().stream()
                .map(this::situacaoDe)
                .toList();
    }

    @Transactional
    public SituacaoDeFerias consultarPorPessoaId(Long pessoaId) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa nao encontrada: " + pessoaId));
        return situacaoDe(pessoa);
    }

    /**
     * Situacao de uma pessoa.
     *
     * Vinculos nao CLT aparecem no painel, mas sem saldo, sem prazo concessivo
     * e sem alerta de vencimento (secao 2.4 do documento de decisoes).
     */
    @Transactional
    public SituacaoDeFerias situacaoDe(Pessoa pessoa) {
        if (!pessoa.possuiRegraDeFerias()) {
            return new SituacaoDeFerias(pessoa, SituacaoFerias.SEM_REGRA, 0, List.of());
        }

        List<PeriodoAquisitivo> periodos = sincronizarPeriodos(pessoa);
        List<SaldoDePeriodo> saldos = calcularSaldos(pessoa, periodos);

        int saldoTotal = saldos.stream().mapToInt(SaldoDePeriodo::saldo).sum();
        SituacaoFerias situacao = SituacaoFerias.REGULAR;

        if (saldos.stream().anyMatch(SaldoDePeriodo::vencido)) {
            situacao = SituacaoFerias.VENCIDO;
        } else if (saldos.stream().anyMatch(SaldoDePeriodo::proximoDoVencimento)) {
            situacao = SituacaoFerias.PROXIMO_DO_VENCIMENTO;
        }

        return new SituacaoDeFerias(pessoa, situacao, saldoTotal, saldos);
    }

    /**
     * Cria os periodos que ja venceram seus doze meses e ainda nao existem.
     *
     * Chamar duas vezes nao duplica nada: so entram periodos cujo numero
     * ainda nao foi registrado. O periodo em curso nao e criado - ele so
     * nasce quando os doze meses se completam.
     */
    private List<PeriodoAquisitivo> sincronizarPeriodos(Pessoa pessoa) {
        List<PeriodoAquisitivo> existentes =
                periodoRepository.findByPessoaIdOrderByDataInicioAsc(pessoa.getId());

        LocalDate hoje = LocalDate.now();
        LocalDate fimDaContagem = (pessoa.getDataSaida() != null && pessoa.getDataSaida().isBefore(hoje))
                ? pessoa.getDataSaida()
                : hoje;

        List<PeriodoAquisitivo> novos = new ArrayList<>();
        int numero = existentes.size() + 1;
        LocalDate inicio = pessoa.getDataEntrada().plusYears(existentes.size());

        // Um periodo so existe depois de completados os doze meses.
        while (!inicio.plusYears(1).isAfter(fimDaContagem)) {
            novos.add(new PeriodoAquisitivo(pessoa, numero, inicio));
            inicio = inicio.plusYears(1);
            numero++;
        }

        if (!novos.isEmpty()) {
            periodoRepository.saveAll(novos);
            existentes = new ArrayList<>(existentes);
            existentes.addAll(novos);
            existentes.sort(Comparator.comparing(PeriodoAquisitivo::getDataInicio));
        }

        return existentes;
    }

    /**
     * Distribui os dias de ferias ja gozados entre os periodos, do mais antigo
     * para o mais novo.
     *
     * A ordem importa: o periodo mais antigo e o que esta mais perto de vencer,
     * entao e ele que deve ser abatido primeiro. Abater pelo mais recente
     * deixaria o saldo em risco intacto e produziria um alerta falso.
     */
    private List<SaldoDePeriodo> calcularSaldos(Pessoa pessoa, List<PeriodoAquisitivo> periodos) {
        int diasRestantesParaDistribuir = totalDeDiasDeFeriasGozados(pessoa);
        LocalDate hoje = LocalDate.now();

        List<SaldoDePeriodo> saldos = new ArrayList<>();
        for (PeriodoAquisitivo periodo : periodos) {
            int direito = periodo.getDiasDeDireito();
            int gozados = Math.min(direito, diasRestantesParaDistribuir);
            diasRestantesParaDistribuir -= gozados;

            int saldo = direito - gozados;
            long diasAteOLimite = ChronoUnit.DAYS.between(hoje, periodo.getDataLimiteGozo());

            boolean vencido = saldo > 0 && periodo.getDataLimiteGozo().isBefore(hoje);
            boolean proximo = saldo > 0 && !vencido && diasAteOLimite <= diasDeAlerta;

            saldos.add(new SaldoDePeriodo(periodo, gozados, saldo, vencido, proximo, diasAteOLimite));
        }
        return saldos;
    }

    /** Soma os dias de todas as ocorrencias de ferias nao canceladas da pessoa. */
    private int totalDeDiasDeFeriasGozados(Pessoa pessoa) {
        return ocorrenciaRepository.findByPessoaIdOrderByDataInicioDesc(pessoa.getId()).stream()
                .filter(o -> o.getTipo() == TipoOcorrencia.FERIAS)
                .filter(o -> o.getSituacao() != SituacaoOcorrencia.CANCELADA)
                .mapToInt(o -> (int) o.getTotalDeDias())
                .sum();
    }
}
