package com.ibmec.kodie;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Carga inicial do banco em memoria.
 *
 * Todos os dados sao ficticios, conforme a exigencia da secao 3.4 do
 * documento de decisoes. Nenhum dado real da KODIE Academy e usado.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final PessoaRepository pessoaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;

    public DataLoader(PessoaRepository pessoaRepository, OcorrenciaRepository ocorrenciaRepository) {
        this.pessoaRepository = pessoaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    @Override
    public void run(String... args) {

        // --- Pessoas: um vinculo de cada tipo ---------------------------------

        Pessoa ana = pessoaRepository.save(new Pessoa(
                "Ana Beatriz Moreira", "11122233344", "ana.moreira@exemplo.com",
                "Coordenadora Pedagogica", TipoVinculo.CLT, LocalDate.of(2024, 12, 4)));

        Pessoa carlos = pessoaRepository.save(new Pessoa(
                "Carlos Eduardo Lima", "22233344455", "carlos.lima@exemplo.com",
                "Instrutor de Programacao", TipoVinculo.CLT, LocalDate.of(2023, 5, 15)));

        Pessoa daniela = pessoaRepository.save(new Pessoa(
                "Daniela Souza Rocha", "33344455566", "daniela.rocha@exemplo.com",
                "Designer Instrucional", TipoVinculo.PJ, LocalDate.of(2025, 2, 1)));

        Pessoa eduardo = pessoaRepository.save(new Pessoa(
                "Eduardo Nunes Pereira", "44455566677", "eduardo.pereira@exemplo.com",
                "Monitor Voluntario", TipoVinculo.VOLUNTARIO, LocalDate.of(2025, 8, 20)));

        // --- Ocorrencias vinculadas a pessoas ---------------------------------

        ocorrenciaRepository.save(new Ocorrencia(
                TipoOcorrencia.FERIAS, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 20),
                SituacaoOcorrencia.CONFIRMADA, "15 dias do periodo 2024/2025", ana));

        ocorrenciaRepository.save(new Ocorrencia(
                TipoOcorrencia.FOLGA, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14),
                SituacaoOcorrencia.CONFIRMADA, "Compensacao de sabado letivo", ana));

        ocorrenciaRepository.save(new Ocorrencia(
                TipoOcorrencia.LICENCA, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16),
                SituacaoOcorrencia.CONFIRMADA, "Licenca medica com atestado", carlos));

        ocorrenciaRepository.save(new Ocorrencia(
                TipoOcorrencia.AUSENCIA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1),
                SituacaoOcorrencia.PLANEJADA, "Consulta medica no periodo da tarde", daniela));

        // --- Ocorrencia sem pessoa vinculada ----------------------------------
        // Feriado da instituicao: vale para o calendario, nao para uma pessoa.

        ocorrenciaRepository.save(new Ocorrencia(
                TipoOcorrencia.FERIADO_TRABALHADO, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20),
                SituacaoOcorrencia.PLANEJADA, "Evento aberto da KODIE Academy", null));

        System.out.println(">>> DataLoader: " + pessoaRepository.count() + " pessoas e "
                + ocorrenciaRepository.count() + " ocorrencias carregadas (dados ficticios)");

        // Eduardo entra sem nenhuma ocorrencia, para a listagem exibir o total zero.
        if (eduardo.getId() == null) {
            throw new IllegalStateException("Falha ao carregar dados iniciais");
        }
    }
}
