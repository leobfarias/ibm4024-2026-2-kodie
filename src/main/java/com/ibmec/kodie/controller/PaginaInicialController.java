package com.ibmec.kodie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Leva a raiz do site direto para o cadastro, em vez de devolver 404. */
@Controller
public class PaginaInicialController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/pessoas/pagina";
    }
}
