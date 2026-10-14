package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.model.TipoVinculo;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Exportacao das informacoes em CSV, para abrir no Excel ou no Google
 * Planilhas (item "exportacao de informacoes" da secao 3.1 do documento
 * de decisoes).
 *
 * Formato pensado para o Excel em portugues: separador ";" (a virgula e o
 * separador decimal), marca BOM para os acentos aparecerem corretos e
 * datas em dd/MM/yyyy.
 */
@Service
public class ExportacaoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String BOM = "﻿";

    private final PessoaService pessoaService;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final FeriasService feriasService;

    public ExportacaoService(PessoaService pessoaService,
                             OcorrenciaRepository ocorrenciaRepository,
                             FeriasService feriasService) {
        this.pessoaService = pessoaService;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.feriasService = feriasService;
    }

    /** Pessoas, respeitando os mesmos filtros da tela de listagem. */
    public byte[] pessoas(String busca, TipoVinculo vinculo, SituacaoVinculo situacao) {
        Csv csv = new Csv("Nome", "CPF", "E-mail", "Cargo", "Vinculo", "Situacao", "Entrada", "Saida", "Ocorrencias");
        for (PessoaResumo r : pessoaService.listarResumo(busca, vinculo, situacao)) {
            Pessoa p = r.pessoa();
            csv.linha(p.getNome(), p.getCpf(), p.getEmail(), p.getCargo(),
                    p.getTipoVinculo().getDescricao(), p.getSituacao().getDescricao(),
                    data(p.getDataEntrada()), data(p.getDataSaida()), String.valueOf(r.totalOcorrencias()));
        }
        return csv.bytes();
    }

    public byte[] ocorrencias() {
        Csv csv = new Csv("Pessoa", "Tipo", "Inicio", "Fim", "Dias", "Situacao", "Observacao");
        List<Ocorrencia> todas = ocorrenciaRepository.findAll().stream()
                .sorted(Comparator.comparing(Ocorrencia::getDataInicio).reversed())
                .toList();
        for (Ocorrencia o : todas) {
            csv.linha(o.getPessoa() == null ? "(sem pessoa vinculada)" : o.getPessoa().getNome(),
                    o.getTipo().getDescricao(), data(o.getDataInicio()), data(o.getDataFim()),
                    String.valueOf(o.getTotalDeDias()), o.getSituacao().getDescricao(), o.getObservacao());
        }
        return csv.bytes();
    }

    /** Situacao de ferias de todo o cadastro, como no painel. */
    public byte[] ferias() {
        Csv csv = new Csv("Pessoa", "Vinculo", "Situacao de ferias", "Saldo (dias)", "Prazo mais proximo", "Periodos completos");
        for (SituacaoDeFerias s : feriasService.montarPainel()) {
            boolean clt = s.pessoa().possuiRegraDeFerias();
            var urgente = s.getPeriodoMaisUrgente();
            csv.linha(s.pessoa().getNome(), s.pessoa().getTipoVinculo().getDescricao(), s.situacao().getDescricao(),
                    clt ? String.valueOf(s.saldoTotal()) : "",
                    urgente == null ? "" : data(urgente.getDataLimiteGozo()),
                    clt ? String.valueOf(s.periodos().size()) : "");
        }
        return csv.bytes();
    }

    private static String data(LocalDate d) {
        return d == null ? "" : d.format(DATA);
    }

    /** Montagem minima de CSV, sem biblioteca externa. */
    private static final class Csv {

        private final StringBuilder texto = new StringBuilder(BOM);

        Csv(String... cabecalho) {
            linha(cabecalho);
        }

        void linha(String... campos) {
            for (int i = 0; i < campos.length; i++) {
                if (i > 0) {
                    texto.append(';');
                }
                texto.append(campo(campos[i]));
            }
            texto.append("\r\n");
        }

        byte[] bytes() {
            return texto.toString().getBytes(StandardCharsets.UTF_8);
        }

        /**
         * Aspas quando o valor tem separador, aspas ou quebra de linha.
         *
         * Valores que comecam com = + - @ ganham um apostrofo na frente: sem
         * isso, o Excel executaria o texto como formula ao abrir o arquivo
         * (injecao de formula em CSV).
         */
        private static String campo(String valor) {
            if (valor == null) {
                return "";
            }
            String v = valor;
            if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
                v = "'" + v;
            }
            if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            return v;
        }
    }
}
