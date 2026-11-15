package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.OcorrenciaService;
import com.ibmec.kodie.service.PessoaService;
import com.ibmec.kodie.service.RecursoNaoEncontradoException;
import com.ibmec.kodie.service.SituacaoDeFerias;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
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

    /**
     * Campos que o formulario de ocorrencia nunca pode preencher.
     *
     *   - id: quem decide o id e o banco. Um id forjado faria o "registrar"
     *     sobrescrever uma ocorrencia que ja existe.
     *   - pessoa.*: o campo "pessoa" escolhe a pessoa pelo id (PessoaConverter).
     *     Sem esta trava, um campo forjado como "pessoa.cpf" alteraria o
     *     cadastro da pessoa escolhida, sem passar pelas regras do PessoaService.
     */
    @InitBinder("novaOcorrencia")
    public void protegerCampos(WebDataBinder binder) {
        binder.setDisallowedFields("id", "pessoa.*");
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

        atributos.addFlashAttribute("mensagem", "Ocorrência registrada com sucesso");

        Pessoa pessoa = novaOcorrencia.getPessoa();
        return (pessoa != null && pessoa.getId() != null)
                ? "redirect:/pessoas/pagina/" + pessoa.getId()
                : "redirect:/ocorrencias/pagina/nova";
    }

    /**
     * Exclusao pedida pela ficha da pessoa (pessoaId) ou pelo calendario
     * (ano e mes). Depois dela, a tela volta para onde o pedido saiu.
     *
     * O destino e montado so com numeros e com o tipo da ocorrencia - nunca
     * com um endereco enviado pelo formulario. Aceitar um "volte para esta
     * URL" abriria um redirecionamento para qualquer site.
     */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,
                          @RequestParam(required = false) Long pessoaId,
                          @RequestParam(required = false) Integer ano,
                          @RequestParam(required = false) Integer mes,
                          @RequestParam(required = false) TipoOcorrencia tipo,
                          RedirectAttributes atributos) {
        try {
            ocorrenciaService.deletar(id);
            atributos.addFlashAttribute("mensagem", "Ocorrência removida");
        } catch (RecursoNaoEncontradoException e) {
            // Clique duplo, ou outra aba ja tinha excluido: nao ha o que mostrar como erro grave.
            atributos.addFlashAttribute("erro", "Esta ocorrência já tinha sido removida");
        }

        if (pessoaId != null) {
            return "redirect:/pessoas/pagina/" + pessoaId;
        }
        if (ano != null && mes != null) {
            // addAttribute vira parametro na URL (?ano=2026&mes=11), ja codificado.
            atributos.addAttribute("ano", ano);
            atributos.addAttribute("mes", mes);
            if (tipo != null) {
                atributos.addAttribute("tipo", tipo);
            }
            return "redirect:/calendario/pagina";
        }
        return "redirect:/ocorrencias/pagina/nova";
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
