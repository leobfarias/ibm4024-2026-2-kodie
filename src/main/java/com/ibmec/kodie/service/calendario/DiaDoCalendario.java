package com.ibmec.kodie.service.calendario;

import com.ibmec.kodie.model.Ocorrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma celula da grade mensal.
 *
 * @param doMes falso para os dias do mes vizinho que completam a primeira e a ultima semana
 */
public record DiaDoCalendario(LocalDate data, boolean doMes, boolean hoje, List<Ocorrencia> ocorrencias) {

    /** Quantas ocorrencias cabem numa celula antes de virar "+N". */
    public static final int VISIVEIS_POR_DIA = 3;

    public boolean isFimDeSemana() {
        return data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public List<Ocorrencia> getVisiveis() {
        return ocorrencias.size() <= VISIVEIS_POR_DIA ? ocorrencias : ocorrencias.subList(0, VISIVEIS_POR_DIA);
    }

    public int getExcedentes() {
        return Math.max(0, ocorrencias.size() - VISIVEIS_POR_DIA);
    }
}
