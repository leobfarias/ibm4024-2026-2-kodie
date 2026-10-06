package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.OcorrenciaService;
import com.ibmec.kodie.service.PessoaService;
import com.ibmec.kodie.service.SituacaoDeFerias;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Tela de registro de ocorrencia: lancamento manual de ferias, folgas,
 * licencas, ausencias e feriados trabalhados.
 */
@Controller
@RequestMapping("/ocorrencias/pagina")
public class OcorrenciaWebController {

    private final OcorrenciaService ocorrenciaService;
    private final PessoaService pessoaService;
    private final FeriasService feriasService;

    public OcorrenciaWebController(OcorrenciaService ocorrenciaService,
                                   PessoaService pessoaService,
                                   FeriasService feriasService) {
        this.ocorrenciaService = ocorrenciaService;
        this.pessoaService = pessoaService;
        this.feriasService = feriasService;
    }

    @ModelAttribute("tiposOcorrencia")
    public TipoOcorrencia[] tiposOcorrencia() {
        return TipoOcorrencia.values();
    }

    @ModelAttribute("situacoesOcorrencia")
    public SituacaoOcorrencia[] situacoesOcorrencia() {
        return SituacaoOcorrencia.values();
    }

    @ModelAttribute("pessoas")
    public List<Pessoa> pessoas() {
        return pessoaService.listarTodos();
    }

    @GetMapping("/nova")
    public String formulario(@RequestParam(required = false) Long pessoaId, Model model) {
        if (!model.containsAttribute("novaOcorrencia")) {
            Ocorrencia nova = new Ocorrencia();
            if (pessoaId != null) {
                pessoaService.buscarPorId(pessoaId).ifPresent(nova::setPessoa);
            }
            model.addAttribute("novaOcorrencia", nova);
        }
        prepararAviso(model);
        return "ocorrencia";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("novaOcorrencia") Ocorrencia novaOcorrencia,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {
            prepararAviso(model);
            return "ocorrencia";
        }

        try {
            ocorrenciaService.criar(novaOcorrencia);
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("dataInicio", "regraDeNegocio", e.getMessage());
            prepararAviso(model);
            return "ocorrencia";
        }

        atributos.addFlashAttribute("mensagem", "Ocorrencia registrada com sucesso");

        Pessoa pessoa = novaOcorrencia.getPessoa();
        return (pessoa != null && pessoa.getId() != null)
                ? "redirect:/pessoas/pagina/" + pessoa.getId()
                : "redirect:/ocorrencias/nova";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,
                          @RequestParam(required = false) Long pessoaId,
                          RedirectAttributes atributos) {
        ocorrenciaService.deletar(id);
        atributos.addFlashAttribute("mensagem", "Ocorrencia removida");
        return (pessoaId != null) ? "redirect:/pessoas/pagina/" + pessoaId : "redirect:/ocorrencias/nova";
    }

    /**
     * Mostra o saldo de ferias da pessoa escolhida enquanto o formulario esta
     * aberto, para evitar um lancamento maior que o saldo disponivel.
     */
    private void prepararAviso(Model model) {
        Ocorrencia ocorrencia = (Ocorrencia) model.getAttribute("novaOcorrencia");
        if (ocorrencia == null || ocorrencia.getPessoa() == null || ocorrencia.getPessoa().getId() == null) {
            return;
        }
        pessoaService.buscarPorId(ocorrencia.getPessoa().getId()).ifPresent(pessoa -> {
            SituacaoDeFerias situacao = feriasService.situacaoDe(pessoa);
            model.addAttribute("situacaoFerias", situacao);
        });
    }
}
