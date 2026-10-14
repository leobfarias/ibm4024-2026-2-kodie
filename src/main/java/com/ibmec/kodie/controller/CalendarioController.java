package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.TipoOcorrencia;
import com.ibmec.kodie.service.CalendarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

/** Calendario mensal de ocorrencias. Disponivel aos dois perfis. */
@Controller
@RequestMapping("/calendario/pagina")
public class CalendarioController {

    private final CalendarioService service;

    public CalendarioController(CalendarioService service) {
        this.service = service;
    }

    @ModelAttribute("tiposOcorrencia")
    public TipoOcorrencia[] tiposOcorrencia() {
        return TipoOcorrencia.values();
    }

    /**
     * Sem parametros, mostra o mes atual. Um mes invalido na URL (mes=13)
     * tambem cai no mes atual, em vez de virar uma pagina de erro.
     */
    @GetMapping
    public String mes(@RequestParam(required = false) Integer ano,
                      @RequestParam(required = false) Integer mes,
                      @RequestParam(required = false) TipoOcorrencia tipo,
                      Model model) {

        YearMonth escolhido = YearMonth.now();
        if (ano != null && mes != null && mes >= 1 && mes <= 12 && ano >= 1900 && ano <= 2100) {
            escolhido = YearMonth.of(ano, mes);
        }

        model.addAttribute("calendario", service.montar(escolhido, tipo));
        model.addAttribute("tipoFiltro", tipo);
        return "calendario";
    }
}
