package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.SituacaoFerias;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.SituacaoDeFerias;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Painel de ferias: a situacao de cada pessoa agrupada nos quatro estados
 * previstos na secao 3.2 do documento de decisoes.
 */
@Controller
@RequestMapping("/ferias/painel")
public class FeriasWebController {

    private final FeriasService service;

    public FeriasWebController(FeriasService service) {
        this.service = service;
    }

    @GetMapping
    public String painel(Model model) {
        Map<SituacaoFerias, List<SituacaoDeFerias>> porSituacao =
                service.montarPainel().stream()
                        .collect(Collectors.groupingBy(SituacaoDeFerias::situacao));

        model.addAttribute("vencidos", porSituacao.getOrDefault(SituacaoFerias.VENCIDO, List.of()));
        model.addAttribute("proximos", porSituacao.getOrDefault(SituacaoFerias.PROXIMO_DO_VENCIMENTO, List.of()));
        model.addAttribute("regulares", porSituacao.getOrDefault(SituacaoFerias.REGULAR, List.of()));
        model.addAttribute("semRegra", porSituacao.getOrDefault(SituacaoFerias.SEM_REGRA, List.of()));
        model.addAttribute("diasDeAlerta", service.getDiasDeAlerta());
        return "ferias";
    }
}
