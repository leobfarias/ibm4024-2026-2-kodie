package com.ibmec.kodie.controller;

import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.SituacaoDeFerias;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ferias")
public class FeriasRestController {

    private final FeriasService service;

    public FeriasRestController(FeriasService service) {
        this.service = service;
    }

    /** Situacao de ferias de todo o cadastro. */
    @GetMapping
    public List<SituacaoDeFerias> painel() {
        return service.montarPainel();
    }

    /** Situacao de uma pessoa. Devolve 404 se ela nao existir. */
    @GetMapping("/pessoa/{id}")
    public SituacaoDeFerias porPessoa(@PathVariable Long id) {
        return service.consultarPorPessoaId(id);
    }
}
