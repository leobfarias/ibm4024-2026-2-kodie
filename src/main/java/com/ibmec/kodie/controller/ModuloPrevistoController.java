package com.ibmec.kodie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Modulos previstos no escopo do documento de decisoes e ainda nao
 * implementados. Aparecem na navegacao para manter visivel o escopo
 * combinado com a cliente, informando honestamente o que falta.
 */
@Controller
@RequestMapping("/modulos")
public class ModuloPrevistoController {

    private static final Map<String, String> TITULOS = Map.of(
            "calendario", "Calendario",
            "indicadores", "Indicadores",
            "acessos", "Acessos");

    @GetMapping("/{modulo}")
    public String previsto(@PathVariable String modulo, Model model) {
        String titulo = TITULOS.get(modulo);
        if (titulo == null) {
            return "redirect:/pessoas/pagina";
        }
        model.addAttribute("titulo", titulo);
        model.addAttribute("modulo", modulo);
        return "modulo-previsto";
    }
}
