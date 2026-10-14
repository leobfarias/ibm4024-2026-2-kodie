/*
 * Alterna entre o tema claro e o escuro e lembra a escolha neste navegador.
 * Sem escolha salva, o tema segue a preferencia do sistema operacional.
 * O tema salvo e aplicado ainda no <head> (ver fragmentos/comum.html),
 * para a pagina nao piscar no tema errado ao carregar.
 */
(function () {
    var CHAVE = 'kodie-tema';

    document.addEventListener('click', function (evento) {
        var botao = evento.target.closest('[data-alternar-tema]');
        if (!botao) {
            return;
        }
        var raiz = document.documentElement;
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
