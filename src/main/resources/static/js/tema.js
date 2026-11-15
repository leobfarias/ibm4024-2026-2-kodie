/*
 * Alterna entre o tema claro e o escuro e lembra a escolha neste navegador.
 * Sem escolha salva, o tema segue a preferencia do sistema operacional.
 *
 * Este arquivo e carregado no <head> SEM "defer" (ver fragmentos/comum.html):
 * o navegador espera ele rodar antes de desenhar a pagina, entao o tema salvo
 * ja vale no primeiro quadro e a pagina nao pisca no tema errado. Antes isso
 * era um <script> escrito dentro do HTML, o que a politica de seguranca de
 * conteudo (CSP, em SegurancaConfig) agora proibe.
 */
(function () {
    var CHAVE = 'kodie-tema';
    var raiz = document.documentElement;

    try {
        var salvo = localStorage.getItem(CHAVE);
        if (salvo) {
            raiz.setAttribute('data-tema', salvo);
        }
    } catch (e) {
        // navegacao privada ou armazenamento bloqueado: segue o sistema operacional
    }

    document.addEventListener('click', function (evento) {
        var botao = evento.target.closest('[data-alternar-tema]');
        if (!botao) {
            return;
        }
        var atual = raiz.getAttribute('data-tema')
            || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro');
        var novo = atual === 'escuro' ? 'claro' : 'escuro';

        raiz.setAttribute('data-tema', novo);
        try {
            localStorage.setItem(CHAVE, novo);
        } catch (e) {
            // navegacao privada ou armazenamento bloqueado: vale so para esta pagina
        }
    });
})();
