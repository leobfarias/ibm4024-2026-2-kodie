package com.ibmec.kodie.controller;

import com.ibmec.kodie.config.SessoesDeUsuario;
import com.ibmec.kodie.service.BackupService;
import com.ibmec.kodie.service.RecursoNaoEncontradoException;
import com.ibmec.kodie.service.backup.ResumoDeBackup;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Backup e restauracao. Exclusivo do administrador (ver SegurancaConfig).
 */
@Controller
@RequestMapping("/backup")
public class BackupController {

    private final BackupService backupService;
    private final SessoesDeUsuario sessoes;

    public BackupController(BackupService backupService, SessoesDeUsuario sessoes) {
        this.backupService = backupService;
        this.sessoes = sessoes;
    }

    /**
     * API: devolve um backup novo a cada chamada, sem grava-lo no servidor.
     * Pensado para automacao - por exemplo, um agendamento em outra maquina
     * que busca o backup e o guarda fora da VPS:
     *   curl -u admin:SENHA https://servidor/backup -o kodie-backup.json
     */
    @GetMapping
    public ResponseEntity<byte[]> baixarBackupAtual() {
        String nome = "kodie-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        return arquivo(nome, backupService.gerarJson("download"));
    }

    // ------------------------------------------------------------------
    // Tela
    // ------------------------------------------------------------------

    @GetMapping("/pagina")
    public String pagina(Model model) {
        model.addAttribute("backups", backupService.listar());
        return "backup";
    }

    @PostMapping("/pagina/gerar")
    public String gerar(RedirectAttributes atributos) {
        String nome = backupService.salvarNoServidor("manual");
        atributos.addFlashAttribute("mensagem", "Backup gerado: " + nome);
        return "redirect:/backup/pagina";
    }

    @GetMapping("/pagina/baixar")
    public ResponseEntity<byte[]> baixar(@RequestParam String arquivo) {
        return arquivo(arquivo, backupService.lerArquivo(arquivo));
    }

    @PostMapping("/pagina/excluir")
    public String excluir(@RequestParam String arquivo, RedirectAttributes atributos) {
        backupService.excluir(arquivo);
        atributos.addFlashAttribute("mensagem", "Backup excluido: " + arquivo);
        return "redirect:/backup/pagina";
    }

    /** Recebe um arquivo do computador do administrador e leva a tela de confirmacao. */
    @PostMapping("/pagina/enviar")
    public String enviar(@RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes atributos) throws IOException {
        if (arquivo.isEmpty()) {
            atributos.addFlashAttribute("erro", "Escolha um arquivo de backup para enviar");
            return "redirect:/backup/pagina";
        }
        try {
            String nome = backupService.receberEnvio(arquivo.getBytes());
            return "redirect:/backup/pagina/restaurar?arquivo=" + nome;
        } catch (IllegalArgumentException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
            return "redirect:/backup/pagina";
        }
    }

    // ------------------------------------------------------------------
    // Restauracao: sempre em duas etapas - analisar, depois confirmar
    // ------------------------------------------------------------------

    @GetMapping("/pagina/restaurar")
    public String confirmarRestauracao(@RequestParam String arquivo, Model model) {
        ResumoDeBackup resumo = backupService.analisar(arquivo);
        model.addAttribute("resumo", resumo);
        return "backup-restaurar";
    }

    @PostMapping("/pagina/restaurar")
    public String restaurar(@RequestParam String arquivo,
                            @RequestParam(defaultValue = "false") boolean confirmo,
                            HttpServletRequest requisicao,
                            RedirectAttributes atributos) {
        if (!confirmo) {
            atributos.addFlashAttribute("erro", "Marque a confirmacao para restaurar");
            return "redirect:/backup/pagina/restaurar?arquivo=" + arquivo;
        }

        try {
            backupService.restaurar(arquivo);
        } catch (IllegalArgumentException e) {
            atributos.addFlashAttribute("erro", e.getMessage());
            return "redirect:/backup/pagina/restaurar?arquivo=" + arquivo;
        }

        // Os usuarios agora sao os do backup: todo mundo entra de novo,
        // inclusive quem restaurou, com as senhas que valiam no backup.
        sessoes.encerrarTodas();
        HttpSession sessao = requisicao.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login?restaurado";
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String backupInexistente(RecursoNaoEncontradoException e, RedirectAttributes atributos) {
        atributos.addFlashAttribute("erro", e.getMessage());
        return "redirect:/backup/pagina";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String nomeInvalido(IllegalArgumentException e, RedirectAttributes atributos) {
        atributos.addFlashAttribute("erro", e.getMessage());
        return "redirect:/backup/pagina";
    }

    private ResponseEntity<byte[]> arquivo(String nome, byte[] conteudo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(conteudo);
    }
}
