package com.ibmec.kodie.controller;

import com.ibmec.kodie.controller.form.SenhaForm;
import com.ibmec.kodie.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Conta do usuario logado. Disponivel para os dois perfis: trocar a
 * propria senha e a unica escrita permitida ao perfil de visualizacao.
 */
@Controller
@RequestMapping("/conta/senha")
public class ContaWebController {

    private final UsuarioService usuarioService;

    public ContaWebController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("senhaForm", new SenhaForm());
        return prepararTela(model);
    }

    @PostMapping
    public String trocar(@Valid @ModelAttribute("senhaForm") SenhaForm form,
                         BindingResult resultado,
                         Principal principal,
                         Model model,
                         RedirectAttributes atributos) {

        if (form.getSenhaAtual() == null || form.getSenhaAtual().isBlank()) {
            resultado.rejectValue("senhaAtual", "obrigatoria", "Informe a senha atual");
        }
        if (form.getNovaSenha() != null && !form.getNovaSenha().equals(form.getConfirmacaoSenha())) {
            resultado.rejectValue("confirmacaoSenha", "naoConfere", "As senhas nao conferem");
        }
        if (resultado.hasErrors()) {
            return prepararTela(model);
        }

        try {
            usuarioService.trocarPropriaSenha(principal.getName(), form.getSenhaAtual(), form.getNovaSenha());
        } catch (IllegalArgumentException e) {
            String campo = e.getMessage().contains("atual nao confere") ? "senhaAtual" : "novaSenha";
            resultado.rejectValue(campo, "regraDeNegocio", e.getMessage());
            return prepararTela(model);
        }

        atributos.addFlashAttribute("mensagem", "Senha alterada com sucesso");
        return "redirect:/pessoas/pagina";
    }

    private String prepararTela(Model model) {
        model.addAttribute("titulo", "Minha senha");
        model.addAttribute("subtitulo", "Troque a senha que voce usa para entrar no sistema.");
        model.addAttribute("acao", "/conta/senha");
        model.addAttribute("voltar", "/pessoas/pagina");
        model.addAttribute("pedirSenhaAtual", true);
        model.addAttribute("menuAtivo", "conta");
        return "senha";
    }
}
