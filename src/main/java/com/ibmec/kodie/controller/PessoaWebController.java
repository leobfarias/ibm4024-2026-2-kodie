package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/pessoas/pagina")
public class PessoaWebController {

    private final PessoaService pessoaService;
    private final FeriasService feriasService;

    public PessoaWebController(PessoaService pessoaService, FeriasService feriasService) {
        this.pessoaService = pessoaService;
        this.feriasService = feriasService;
    }

    @ModelAttribute("tiposVinculo")
    public TipoVinculo[] tiposVinculo() {
        return TipoVinculo.values();
    }

    @ModelAttribute("situacoesVinculo")
    public SituacaoVinculo[] situacoesVinculo() {
        return SituacaoVinculo.values();
    }

    // ------------------------------------------------------------------
    // Lista de pessoas, com busca e filtros
    // ------------------------------------------------------------------

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) TipoVinculo vinculo,
                         @RequestParam(required = false) SituacaoVinculo situacao,
                         Model model) {

        model.addAttribute("resumos", pessoaService.listarResumo(busca, vinculo, situacao));
        model.addAttribute("busca", busca);
        model.addAttribute("vinculoFiltro", vinculo);
        model.addAttribute("situacaoFiltro", situacao);
        model.addAttribute("totalCadastrado", pessoaService.contarTodas());
        model.addAttribute("totalAtivo", pessoaService.contarAtivas());
        model.addAttribute("filtrando", busca != null || vinculo != null || situacao != null);

        if (!model.containsAttribute("novaPessoa")) {
            model.addAttribute("novaPessoa", new Pessoa());
        }
        return "pessoas";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("novaPessoa") Pessoa novaPessoa,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {
            return recarregarLista(model);
        }

        try {
            pessoaService.criar(novaPessoa);
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("cpf", "regraDeNegocio", e.getMessage());
            return recarregarLista(model);
        }

        atributos.addFlashAttribute("mensagem", "Pessoa cadastrada com sucesso");
        return "redirect:/pessoas/pagina";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes atributos) {
        pessoaService.deletar(id);
        atributos.addFlashAttribute("mensagem", "Pessoa removida do cadastro");
        return "redirect:/pessoas/pagina";
    }

    // ------------------------------------------------------------------
    // Ficha individual
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    public String ficha(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        Optional<Pessoa> pessoa = pessoaService.buscarPorId(id);
        if (pessoa.isEmpty()) {
            atributos.addFlashAttribute("erro", "Pessoa nao encontrada: " + id);
            return "redirect:/pessoas/pagina";
        }

        model.addAttribute("pessoa", pessoa.get());
        model.addAttribute("ocorrencias", pessoaService.listarOcorrencias(id));
        model.addAttribute("ferias", feriasService.situacaoDe(pessoa.get()));
        return "ficha";
    }

    /** Repoe o que o GET da lista colocaria, para renderizar a view com os erros. */
    private String recarregarLista(Model model) {
        model.addAttribute("resumos", pessoaService.listarResumo());
        model.addAttribute("totalCadastrado", pessoaService.contarTodas());
        model.addAttribute("totalAtivo", pessoaService.contarAtivas());
        model.addAttribute("filtrando", false);
        return "pessoas";
    }
}
