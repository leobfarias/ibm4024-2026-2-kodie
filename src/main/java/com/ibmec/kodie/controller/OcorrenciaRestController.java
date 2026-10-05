package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.Ocorrencia;
import com.ibmec.kodie.service.OcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ocorrencias")
public class OcorrenciaRestController {

    private final OcorrenciaService service;

    public OcorrenciaRestController(OcorrenciaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ocorrencia criar(@Valid @RequestBody Ocorrencia ocorrencia) {
        return service.criar(ocorrencia);
    }

    @GetMapping
    public List<Ocorrencia> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ocorrencia> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Ocorrencia atualizar(@PathVariable Long id, @Valid @RequestBody Ocorrencia dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
