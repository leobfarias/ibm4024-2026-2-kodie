package com.ibmec.kodie.controller;

import com.ibmec.kodie.config.SessoesDeUsuario;
import com.ibmec.kodie.controller.form.NovoUsuarioForm;
import com.ibmec.kodie.controller.form.SenhaForm;
import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.model.Usuario;
import com.ibmec.kodie.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

/**
 * Gestao de acessos: exclusiva do administrador (ver SegurancaConfig).
 */
@Controller
@RequestMapping("/usuarios/pagina")
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final SessoesDeUsuario sessoes;

    public UsuarioWebController(UsuarioService usuarioService, SessoesDeUsuario sessoes) {
        this.usuarioService = usuarioService;
        this.sessoes = sessoes;
    }

    @ModelAttribute("perfis")
    public PerfilAcesso[] perfis() {
        return PerfilAcesso.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        if (!model.containsAttribute("novoUsuario")) {
            model.addAttribute("novoUsuario", new NovoUsuarioForm());
        }
        return "usuarios";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("novoUsuario") NovoUsuarioForm form,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes atributos) {

        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmacaoSenha())) {
            resultado.rejectValue("confirmacaoSenha", "naoConfere", "As senhas nao conferem");
        }
        if (resultado.hasErrors()) {
            return recarregar(model, form);
        }

        try {
            usuarioService.criar(form.getLogin(), form.getNome(), form.getSenha(), form.getPerfil());
        } catch (IllegalArgumentException e) {
            resultado.rejectValue("login", "regraDeNegocio", e.getMessage());
            return recarregar(model, form);
        }

        atributos.addFlashAttribute("mensagem", "Usuario " + form.getLogin() + " criado com sucesso");
        return "redirect:/usuarios/pagina";
    }

    @PostMapping("/{id}/perfil")
    public String alterarPerfil(@PathVariable Long id,
                                @RequestParam PerfilAcesso perfil,
                                Principal principal,
                                RedirectAttributes atributos) {
        try {
            usuarioService.alterarPerfil(id, perfil, principal.getName());
            encerrarSessoesDe(id);
            atributos.addFlashAttribute("mensagem", "Perfil alterado para " + perfil.getDescricao());
        } catch (IllegalArgumentException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios/pagina";
    }

    @PostMapping("/{id}/situacao")
    public String alterarSituacao(@PathVariable Long id,
                                  @RequestParam boolean ativo,
                                  Principal principal,
                                  RedirectAttributes atributos) {
        try {
            usuarioService.alterarSituacao(id, ativo, principal.getName());
            encerrarSessoesDe(id);
            atributos.addFlashAttribute("mensagem", ativo ? "Usuario reativado" : "Usuario desativado");
        } catch (IllegalArgumentException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios/pagina";
    }

    // ------------------------------------------------------------------
    // Redefinicao de senha de outro usuario
    // ------------------------------------------------------------------

    @GetMapping("/{id}/senha")
    public String formularioSenha(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        Optional<Usuario> usuario = usuarioService.buscarPorId(id);
        if (usuario.isEmpty()) {
            atributos.addFlashAttribute("erro", "Usuario nao encontrado: " + id);
            return "redirect:/usuarios/pagina";
        }
        model.addAttribute("senhaForm", new SenhaForm());
        return prepararTelaDeSenha(model, usuario.get());
    }

    @PostMapping("/{id}/senha")
    public String redefinirSenha(@PathVariable Long id,
                                 @Valid @ModelAttribute("senhaForm") SenhaForm form,
                                 BindingResult resultado,
                                 Model model,
                                 RedirectAttributes atributos) {

        Optional<Usuario> usuario = usuarioService.buscarPorId(id);
        if (usuario.isEmpty()) {
            atributos.addFlashAttribute("erro", "Usuario nao encontrado: " + id);
            return "redirect:/usuarios/pagina";
        }

        if (form.getNovaSenha() != null && !form.getNovaSenha().equals(form.getConfirmacaoSenha())) {
            resultado.rejectValue("confirmacaoSenha", "naoConfere", "As senhas nao conferem");
        }
        if (resultado.hasErrors()) {
            return prepararTelaDeSenha(model, usuario.get());
        }

        usuarioService.redefinirSenha(id, form.getNovaSenha());
        sessoes.encerrar(usuario.get().getLogin());
        atributos.addFlashAttribute("mensagem", "Senha de " + usuario.get().getLogin() + " redefinida");
        return "redirect:/usuarios/pagina";
    }

    /** O novo acesso vale imediatamente: a pessoa precisa entrar de novo. */
    private void encerrarSessoesDe(Long id) {
        usuarioService.buscarPorId(id).ifPresent(u -> sessoes.encerrar(u.getLogin()));
    }

    private String prepararTelaDeSenha(Model model, Usuario usuario) {
        model.addAttribute("titulo", "Redefinir senha");
        model.addAttribute("subtitulo", "Nova senha para " + usuario.getNome() + " (" + usuario.getLogin() + ")");
        model.addAttribute("acao", "/usuarios/pagina/" + usuario.getId() + "/senha");
        model.addAttribute("voltar", "/usuarios/pagina");
        model.addAttribute("pedirSenhaAtual", false);
        model.addAttribute("menuAtivo", "acessos");
        return "senha";
    }

    private String recarregar(Model model, NovoUsuarioForm form) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("novoUsuario", form);
        return "usuarios";
    }
}
