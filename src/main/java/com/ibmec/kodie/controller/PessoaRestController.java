package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pessoas")
public class PessoaRestController {

    private final PessoaService service;

    public PessoaRestController(PessoaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pessoa criar(@Valid @RequestBody Pessoa pessoa) {
        return service.criar(pessoa);
    }

    @GetMapping
    public List<Pessoa> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pessoa> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Pessoa atualizar(@PathVariable Long id, @Valid @RequestBody Pessoa dados) {
        return service.atualizar(id, dados);
    }

    /**
     * Ocorrencias de uma pessoa. Devolve 404 quando a pessoa nao existe,
     * e nao uma lista vazia com 200.
     */
    @GetMapping("/{id}/ocorrencias")
    public List<Ocorrencia> listarOcorrencias(@PathVariable Long id) {
        return service.listarOcorrencias(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
