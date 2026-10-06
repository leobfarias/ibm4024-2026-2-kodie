package com.ibmec.kodie.controller;

import com.ibmec.kodie.model.SituacaoFerias;
import com.ibmec.kodie.model.TipoVinculo;
import com.ibmec.kodie.service.FeriasService;
import com.ibmec.kodie.service.SituacaoDeFerias;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Painel de ferias: a situacao de cada pessoa nos quatro estados previstos
 * na secao 3.2 do documento de decisoes.
 */
@Controller
@RequestMapping("/ferias/painel")
public class FeriasWebController {

    private final FeriasService service;

    public FeriasWebController(FeriasService service) {
        this.service = service;
    }

    @ModelAttribute("tiposVinculo")
    public TipoVinculo[] tiposVinculo() {
        return TipoVinculo.values();
    }

    @ModelAttribute("situacoesFerias")
    public SituacaoFerias[] situacoesFerias() {
        return SituacaoFerias.values();
    }

    @GetMapping
    public String painel(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) TipoVinculo vinculo,
                         @RequestParam(required = false) SituacaoFerias situacao,
                         Model model) {

        List<SituacaoDeFerias> todas = service.montarPainel();

        // Os indicadores contam o cadastro inteiro: eles mostram a posicao
        // geral e nao devem mudar conforme os filtros da tabela.
        model.addAttribute("totalRegular", contar(todas, SituacaoFerias.REGULAR));
        model.addAttribute("totalProximo", contar(todas, SituacaoFerias.PROXIMO_DO_VENCIMENTO));
        model.addAttribute("totalVencido", contar(todas, SituacaoFerias.VENCIDO));
        model.addAttribute("totalSemRegra", contar(todas, SituacaoFerias.SEM_REGRA));

        String termo = (busca == null || busca.isBlank()) ? null : busca.trim().toLowerCase();

        List<SituacaoDeFerias> filtradas = todas.stream()
                .filter(s -> termo == null || s.pessoa().getNome().toLowerCase().contains(termo))
                .filter(s -> vinculo == null || s.pessoa().getTipoVinculo() == vinculo)
                .filter(s -> situacao == null || s.situacao() == situacao)
                .toList();

        model.addAttribute("situacoes", filtradas);
        model.addAttribute("busca", busca);
        model.addAttribute("vinculoFiltro", vinculo);
        model.addAttribute("situacaoFiltro", situacao);
        model.addAttribute("filtrando", busca != null || vinculo != null || situacao != null);
        model.addAttribute("diasDeAlerta", service.getDiasDeAlerta());
        model.addAttribute("hoje", LocalDate.now());

        return "ferias";
    }

    private long contar(List<SituacaoDeFerias> todas, SituacaoFerias situacao) {
        return todas.stream().filter(s -> s.situacao() == situacao).count();
    }
}
