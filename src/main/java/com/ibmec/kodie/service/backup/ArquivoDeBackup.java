package com.ibmec.kodie.service.backup;

import com.ibmec.kodie.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conteudo de um arquivo de backup do KODIE (JSON).
 *
 * Usa registros proprios, e nao as entidades, de proposito: o formato do
 * arquivo e um contrato. Se uma entidade ganhar um campo, os backups antigos
 * precisam continuar legiveis, e isso so e possivel com um formato que muda
 * apenas quando se decide muda-lo - e entao "formato" e incrementado.
 *
 * O arquivo contem dados pessoais e os hashes das senhas: deve ser guardado
 * com o mesmo cuidado que o proprio banco.
 */
public record ArquivoDeBackup(
        int formato,
        LocalDateTime geradoEm,
        String versaoDoSchema,
        String motivo,
        Contagens contagens,
        List<UsuarioRegistro> usuarios,
        List<PessoaRegistro> pessoas,
        List<OcorrenciaRegistro> ocorrencias,
        List<PeriodoRegistro> periodosAquisitivos) {

    public static final int FORMATO_ATUAL = 1;

    public record Contagens(int usuarios, int pessoas, int ocorrencias, int periodosAquisitivos) {
    }

    public record UsuarioRegistro(Long id, String login, String nome, String senhaHash,
                                  PerfilAcesso perfil, boolean ativo, LocalDateTime criadoEm) {
    }

    public record PessoaRegistro(Long id, String nome, String cpf, String email, String cargo,
                                 TipoVinculo tipoVinculo, SituacaoVinculo situacao,
                                 LocalDate dataEntrada, LocalDate dataSaida) {
    }

    public record OcorrenciaRegistro(Long id, TipoOcorrencia tipo, LocalDate dataInicio, LocalDate dataFim,
                                     SituacaoOcorrencia situacao, String observacao, Long pessoaId) {
    }

    public record PeriodoRegistro(Long id, Long pessoaId, Integer numero, LocalDate dataInicio,
                                  LocalDate dataFim, Integer diasDeDireito, LocalDate dataLimiteGozo) {
    }
}
