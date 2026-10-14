package com.ibmec.kodie.controller;

import com.ibmec.kodie.config.UsuarioAutenticado;
import com.ibmec.kodie.model.SituacaoFerias;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.IndicadoresService;
import com.ibmec.kodie.service.SituacaoDeFerias;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pagina inicial: o resumo do dia. Reaproveita os servicos de indicadores e
 * de ferias - nao calcula nada que nao exista em outra tela.
 */
@Controller
public class PaginaInicialController {

    /** Quantos alertas de ferias cabem no resumo; o painel completo mostra todos. */
    private static final int ALERTAS_NO_RESUMO = 6;
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final IndicadoresService indicadoresService;
    private final FeriasService feriasService;

    public PaginaInicialController(IndicadoresService indicadoresService, FeriasService feriasService) {
        this.indicadoresService = indicadoresService;
        this.feriasService = feriasService;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        // Vencidas primeiro; dentro de cada grupo, o prazo mais proximo primeiro.
        List<SituacaoDeFerias> alertas = feriasService.montarPainel().stream()
                .filter(s -> s.situacao() == SituacaoFerias.VENCIDO || s.situacao() == SituacaoFerias.PROXIMO_DO_VENCIMENTO)
                .sorted(Comparator.comparing((SituacaoDeFerias s) -> s.situacao() != SituacaoFerias.VENCIDO)
                        .thenComparing(s -> s.getPeriodoMaisUrgente().getDataLimiteGozo()))
                .toList();

        model.addAttribute("painel", indicadoresService.montar());
        model.addAttribute("alertas", alertas.stream().limit(ALERTAS_NO_RESUMO).toList());
        model.addAttribute("totalDeAlertas", alertas.size());
        model.addAttribute("saudacao", saudacao());
        model.addAttribute("primeiroNome", usuario == null ? "" : usuario.getNome().trim().split("\\s+")[0]);
        model.addAttribute("dataPorExtenso", porExtenso(LocalDate.now(SAO_PAULO)));
        return "inicio";
    }

    /** "Quarta-feira, 14 de outubro de 2026", sempre em portugues, independente do navegador. */
    private static String porExtenso(LocalDate data) {
        String texto = data.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.of("pt", "BR")));
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String saudacao() {
        int hora = LocalTime.now(SAO_PAULO).getHour();
        if (hora < 12) {
            return "Bom dia";
        }
        return hora < 18 ? "Boa tarde" : "Boa noite";
    }
}
