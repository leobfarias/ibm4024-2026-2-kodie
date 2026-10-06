package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.OcorrenciaService;
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
    private final OcorrenciaService ocorrenciaService;
    private final FeriasService feriasService;

    public PessoaWebController(PessoaService pessoaService,
                               OcorrenciaService ocorrenciaService,
                               FeriasService feriasService) {
        this.pessoaService = pessoaService;
        this.ocorrenciaService = ocorrenciaService;
        this.feriasService = feriasService;
    }

    /** Listas de opcoes disponiveis em todas as telas deste controller. */
    @ModelAttribute("tiposVinculo")
    public TipoVinculo[] tiposVinculo() {
        return TipoVinculo.values();
    }

    @ModelAttribute("situacoesVinculo")
    public SituacaoVinculo[] situacoesVinculo() {
        return SituacaoVinculo.values();
    }

    @ModelAttribute("tiposOcorrencia")
    public TipoOcorrencia[] tiposOcorrencia() {
        return TipoOcorrencia.values();
    }

    @ModelAttribute("situacoesOcorrencia")
    public SituacaoOcorrencia[] situacoesOcorrencia() {
        return SituacaoOcorrencia.values();
    }

    // ------------------------------------------------------------------
    // Tela 1: lista de pessoas com formulario de cadastro
    // ------------------------------------------------------------------

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("resumos", pessoaService.listarResumo());
        model.addAttribute("novaPessoa", new Pessoa());
        return "pessoas";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("novaPessoa") Pessoa novaPessoa,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {
            model.addAttribute("resumos", pessoaService.listarResumo());
            return "pessoas";
        }

        try {
            pessoaService.criar(novaPessoa);
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("cpf", "regraDeNegocio", e.getMessage());
            model.addAttribute("resumos", pessoaService.listarResumo());
            return "pessoas";
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
    // Tela 2: ficha individual com as ocorrencias da pessoa
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
        if (!model.containsAttribute("novaOcorrencia")) {
            model.addAttribute("novaOcorrencia", new Ocorrencia());
        }
        return "ficha";
    }

    @PostMapping("/{id}/ocorrencias")
    public String registrarOcorrencia(@PathVariable Long id,
                                      @Valid @ModelAttribute("novaOcorrencia") Ocorrencia novaOcorrencia,
                                      BindingResult resultado,
                                      Model model,
                                      RedirectAttributes atributos) {

        Optional<Pessoa> pessoa = pessoaService.buscarPorId(id);
        if (pessoa.isEmpty()) {
            atributos.addFlashAttribute("erro", "Pessoa nao encontrada: " + id);
            return "redirect:/pessoas/pagina";
        }

        novaOcorrencia.setPessoa(pessoa.get());

        if (resultado.hasErrors()) {
            return recarregarFicha(model, pessoa.get(), id);
        }

        try {
            ocorrenciaService.criar(novaOcorrencia);
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("dataInicio", "regraDeNegocio", e.getMessage());
            return recarregarFicha(model, pessoa.get(), id);
        }

        atributos.addFlashAttribute("mensagem", "Ocorrencia registrada com sucesso");
        return "redirect:/pessoas/pagina/" + id;
    }

    @PostMapping("/{pessoaId}/ocorrencias/{ocorrenciaId}/excluir")
    public String excluirOcorrencia(@PathVariable Long pessoaId,
                                    @PathVariable Long ocorrenciaId,
                                    RedirectAttributes atributos) {
        ocorrenciaService.deletar(ocorrenciaId);
        atributos.addFlashAttribute("mensagem", "Ocorrencia removida");
        return "redirect:/pessoas/pagina/" + pessoaId;
    }

    /** Repoe o que o GET da ficha colocaria, para renderizar a view com os erros. */
    private String recarregarFicha(Model model, Pessoa pessoa, Long id) {
        model.addAttribute("pessoa", pessoa);
        model.addAttribute("ocorrencias", pessoaService.listarOcorrencias(id));
        model.addAttribute("ferias", feriasService.situacaoDe(pessoa));
        return "ficha";
    }
}
