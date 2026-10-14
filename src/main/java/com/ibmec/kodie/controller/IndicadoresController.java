package com.ibmec.kodie.controller;

import com.ibmec.kodie.service.IndicadoresService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Indicadores basicos do cadastro. Disponivel aos dois perfis. */
@Controller
@RequestMapping("/indicadores/pagina")
public class IndicadoresController {

    private final IndicadoresService service;

    public IndicadoresController(IndicadoresService service) {
        this.service = service;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute("painel", service.montar());
        return "indicadores";
    }
}
