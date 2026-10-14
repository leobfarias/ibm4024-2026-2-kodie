package com.ibmec.kodie.service.calendario;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.TipoOcorrencia;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Um mes pronto para exibir: a grade em semanas (domingo a sabado) e a
 * lista das ocorrencias que tocam o mes.
 */
public record MesDoCalendario(YearMonth mes,
                              List<List<DiaDoCalendario>> semanas,
                              List<Ocorrencia> ocorrencias,
                              Map<TipoOcorrencia, Long> quantidadePorTipo) {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** "Outubro de 2026" */
    public String getTitulo() {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " de " + mes.getYear();
    }

    public YearMonth getAnterior() {
        return mes.minusMonths(1);
    }

    public YearMonth getProximo() {
        return mes.plusMonths(1);
    }

    public boolean isMesAtual() {
        return mes.equals(YearMonth.now());
    }
}
