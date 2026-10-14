package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.SituacaoVinculo;
import com.ibmec.kodie.model.TipoVinculo;
import com.ibmec.kodie.service.ExportacaoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Downloads em CSV. Liberados aos dois perfis: exportar e consultar, e o
 * perfil de visualizacao ja ve essas mesmas informacoes nas telas.
 */
@RestController
@RequestMapping("/exportacao")
public class ExportacaoController {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final ExportacaoService service;

    public ExportacaoController(ExportacaoService service) {
        this.service = service;
    }

    @GetMapping("/pessoas.csv")
    public ResponseEntity<byte[]> pessoas(@RequestParam(required = false) String busca,
                                          @RequestParam(required = false) TipoVinculo vinculo,
                                          @RequestParam(required = false) SituacaoVinculo situacao) {
        return baixar("pessoas", service.pessoas(busca, vinculo, situacao));
    }

    @GetMapping("/ocorrencias.csv")
    public ResponseEntity<byte[]> ocorrencias() {
        return baixar("ocorrencias", service.ocorrencias());
    }

    @GetMapping("/ferias.csv")
    public ResponseEntity<byte[]> ferias() {
        return baixar("ferias", service.ferias());
    }

    private ResponseEntity<byte[]> baixar(String nome, byte[] conteudo) {
        String arquivo = "kodie-" + nome + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(arquivo).build().toString())
                .body(conteudo);
    }
}
