package com.ibmec.kodie.service;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.SituacaoOcorrencia;
import com.ibmec.kodie.model.TipoOcorrencia;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.service.calendario.DiaDoCalendario;
import com.ibmec.kodie.service.calendario.MesDoCalendario;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calendario de ocorrencias (secao 3.1 do documento de decisoes).
 *
 * Como os alertas e os indicadores, e uma leitura derivada do cadastro
 * central: nao armazena nada proprio (secao 2.1).
 */
@Service
public class CalendarioService {

    private final OcorrenciaRepository ocorrenciaRepository;

    public CalendarioService(OcorrenciaRepository ocorrenciaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    /**
     * @param tipo filtra um tipo de ocorrencia; nulo mostra todos
     */
    public MesDoCalendario montar(YearMonth mes, TipoOcorrencia tipo) {
        // A grade comeca no domingo da primeira semana e termina no sabado da
        // ultima, entao inclui alguns dias dos meses vizinhos.
        LocalDate primeiroDoMes = mes.atDay(1);
        LocalDate ultimoDoMes = mes.atEndOfMonth();
        LocalDate inicioDaGrade = primeiroDoMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate fimDaGrade = ultimoDoMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        // Uma unica consulta ao banco para a grade inteira.
        List<Ocorrencia> daGrade = ocorrenciaRepository
                .buscarNoPeriodo(inicioDaGrade, fimDaGrade, SituacaoOcorrencia.CANCELADA).stream()
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .toList();

        LocalDate hoje = LocalDate.now();
        List<List<DiaDoCalendario>> semanas = new ArrayList<>();
        List<DiaDoCalendario> semana = new ArrayList<>();

        for (LocalDate dia = inicioDaGrade; !dia.isAfter(fimDaGrade); dia = dia.plusDays(1)) {
            LocalDate d = dia;
            List<Ocorrencia> doDia = daGrade.stream()
                    .filter(o -> !o.getDataInicio().isAfter(d) && !o.getDataFim().isBefore(d))
                    .toList();
            semana.add(new DiaDoCalendario(d, YearMonth.from(d).equals(mes), d.equals(hoje), doDia));
            if (semana.size() == 7) {
                semanas.add(semana);
                semana = new ArrayList<>();
            }
        }

        List<Ocorrencia> doMes = daGrade.stream()
                .filter(o -> !o.getDataInicio().isAfter(ultimoDoMes) && !o.getDataFim().isBefore(primeiroDoMes))
                .toList();

        Map<TipoOcorrencia, Long> porTipo = new EnumMap<>(TipoOcorrencia.class);
        porTipo.putAll(doMes.stream().collect(Collectors.groupingBy(Ocorrencia::getTipo, Collectors.counting())));

        return new MesDoCalendario(mes, semanas, doMes, porTipo);
    }
}
