package com.ibmec.kodie.service;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import com.ibmec.kodie.service.indicadores.Fatia;
import com.ibmec.kodie.service.indicadores.OcorrenciasPorTipo;
import com.ibmec.kodie.service.indicadores.PainelDeIndicadores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Indicadores basicos (secao 3.1 do documento de decisoes).
 *
 * Ficam de fora, de proposito:
 *   - tempo de casa: excluido do MVP pela cliente (secao 3.3);
 *   - diversidade: o cadastro nao tem esses campos, e quais campos entram
 *     no MVP ainda depende da cliente (secao 5). Quando entrarem, serao
 *     exibidos apenas de forma agregada (secao 3.4).
 */
@Service
public class IndicadoresService {

    /** Tipos que tiram a pessoa do trabalho. Feriado trabalhado e o contrario. */
    private static final Set<TipoOcorrencia> TIPOS_DE_AUSENCIA = EnumSet.of(
            TipoOcorrencia.FERIAS, TipoOcorrencia.FOLGA, TipoOcorrencia.LICENCA, TipoOcorrencia.AUSENCIA);

    private static final int DIAS_A_FRENTE = 30;

    private final PessoaRepository pessoaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final FeriasService feriasService;

    public IndicadoresService(PessoaRepository pessoaRepository,
                              OcorrenciaRepository ocorrenciaRepository,
                              FeriasService feriasService) {
        this.pessoaRepository = pessoaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.feriasService = feriasService;
    }

    @Transactional
    public PainelDeIndicadores montar() {
        LocalDate hoje = LocalDate.now();
        List<Pessoa> pessoas = pessoaRepository.findAll();

        // --- Quadro de pessoas ------------------------------------------------

        Map<SituacaoVinculo, Long> porSituacao = contar(pessoas, Pessoa::getSituacao, SituacaoVinculo.class);
        long total = pessoas.size();
        List<Fatia> fatiasSituacao = List.of(
                Fatia.de("Ativo", porSituacao.get(SituacaoVinculo.ATIVO), total, "verde"),
                Fatia.de("Afastado", porSituacao.get(SituacaoVinculo.AFASTADO), total, "ambar"),
                Fatia.de("Desligado", porSituacao.get(SituacaoVinculo.DESLIGADO), total, "vermelha"),
                Fatia.de("Inativo", porSituacao.get(SituacaoVinculo.INATIVO), total, "neutra"));

        List<Pessoa> vigentes = pessoas.stream()
                .filter(p -> p.getSituacao() == SituacaoVinculo.ATIVO || p.getSituacao() == SituacaoVinculo.AFASTADO)
                .toList();
        Map<TipoVinculo, Long> porVinculo = contar(vigentes, Pessoa::getTipoVinculo, TipoVinculo.class);
        List<Fatia> fatiasVinculo = Arrays.stream(TipoVinculo.values())
                .map(t -> Fatia.de(t.getDescricao(), porVinculo.get(t), vigentes.size(),
                        t == TipoVinculo.CLT ? "azul" : "neutra"))
                .toList();

        // --- Ferias -----------------------------------------------------------

        List<SituacaoDeFerias> ferias = feriasService.montarPainel();
        Map<SituacaoFerias, Long> porEstado = contar(ferias, SituacaoDeFerias::situacao, SituacaoFerias.class);
        List<Fatia> fatiasFerias = List.of(
                Fatia.de("Regular", porEstado.get(SituacaoFerias.REGULAR), ferias.size(), "verde"),
                Fatia.de("A vencer", porEstado.get(SituacaoFerias.PROXIMO_DO_VENCIMENTO), ferias.size(), "ambar"),
                Fatia.de("Vencido", porEstado.get(SituacaoFerias.VENCIDO), ferias.size(), "vermelha"),
                Fatia.de("Sem regra CLT", porEstado.get(SituacaoFerias.SEM_REGRA), ferias.size(), "neutra"));
        int saldoTotal = ferias.stream().mapToInt(SituacaoDeFerias::saldoTotal).sum();
        int diasVencidos = ferias.stream()
                .flatMap(s -> s.periodos().stream())
                .filter(p -> p.vencido())
                .mapToInt(p -> p.saldo())
                .sum();

        // --- Ocorrencias do ano ----------------------------------------------

        int ano = hoje.getYear();
        LocalDate inicioDoAno = LocalDate.of(ano, 1, 1);
        LocalDate fimDoAno = LocalDate.of(ano, 12, 31);
        List<Ocorrencia> doAno = ocorrenciaRepository.buscarNoPeriodo(inicioDoAno, fimDoAno, SituacaoOcorrencia.CANCELADA);

        Map<TipoOcorrencia, long[]> acumulado = new EnumMap<>(TipoOcorrencia.class);
        for (TipoOcorrencia t : TipoOcorrencia.values()) {
            acumulado.put(t, new long[]{0, 0});           // [quantidade, dias]
        }
        for (Ocorrencia o : doAno) {
            LocalDate inicio = o.getDataInicio().isBefore(inicioDoAno) ? inicioDoAno : o.getDataInicio();
            LocalDate fim = o.getDataFim().isAfter(fimDoAno) ? fimDoAno : o.getDataFim();
            long[] a = acumulado.get(o.getTipo());
            a[0]++;
            a[1] += ChronoUnit.DAYS.between(inicio, fim) + 1;
        }
        long diasNoAno = acumulado.values().stream().mapToLong(a -> a[1]).sum();
        List<OcorrenciasPorTipo> porTipo = acumulado.entrySet().stream()
                .map(e -> new OcorrenciasPorTipo(e.getKey(), e.getValue()[0], e.getValue()[1],
                        diasNoAno == 0 ? 0 : (int) Math.round(e.getValue()[1] * 100.0 / diasNoAno)))
                .toList();

        // --- Hoje e proximos dias --------------------------------------------

        List<Ocorrencia> ausentes = ocorrenciaRepository.buscarNoPeriodo(hoje, hoje, SituacaoOcorrencia.CANCELADA).stream()
                .filter(o -> o.getPessoa() != null && TIPOS_DE_AUSENCIA.contains(o.getTipo()))
                .toList();

        List<Ocorrencia> proximas = ocorrenciaRepository
                .buscarNoPeriodo(hoje.plusDays(1), hoje.plusDays(DIAS_A_FRENTE), SituacaoOcorrencia.CANCELADA).stream()
                .filter(o -> o.getDataInicio().isAfter(hoje))      // so as que ainda vao comecar
                .toList();

        // --- Movimentacao -----------------------------------------------------

        LocalDate umAnoAtras = hoje.minusMonths(12);
        long admissoes = pessoas.stream()
                .filter(p -> !p.getDataEntrada().isBefore(umAnoAtras) && !p.getDataEntrada().isAfter(hoje))
                .count();
        long desligamentos = pessoas.stream()
                .filter(p -> p.getDataSaida() != null
                        && !p.getDataSaida().isBefore(umAnoAtras) && !p.getDataSaida().isAfter(hoje))
                .count();

        return new PainelDeIndicadores(hoje, total, vigentes.size(),
                fatiasSituacao, fatiasVinculo,
                fatiasFerias, porEstado.get(SituacaoFerias.VENCIDO), saldoTotal, diasVencidos,
                ano, porTipo, doAno.size(),
                ausentes, proximas,
                admissoes, desligamentos);
    }

    /** Conta por categoria, garantindo zero para as categorias sem nenhum item. */
    private static <T, E extends Enum<E>> Map<E, Long> contar(Collection<T> itens,
                                                              java.util.function.Function<T, E> categoria,
                                                              Class<E> tipo) {
        Map<E, Long> contagem = new EnumMap<>(tipo);
        for (E e : tipo.getEnumConstants()) {
            contagem.put(e, 0L);
        }
        for (T item : itens) {
            contagem.merge(categoria.apply(item), 1L, Long::sum);
        }
        return contagem;
    }
}
