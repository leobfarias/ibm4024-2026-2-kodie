/*
 * Comportamentos das telas, sem nenhum JavaScript escrito dentro do HTML.
 *
 * A politica de seguranca de conteudo (CSP, em SegurancaConfig) so deixa
 * rodar scripts vindos de arquivos do proprio sistema. Atributos como
 * onsubmit="...", onchange="..." e links href="javascript:..." sao
 * bloqueados pelo navegador - e e justamente esse bloqueio que impede um
 * script injetado de rodar. No lugar deles, o HTML marca o elemento com um
 * atributo data-* e este arquivo faz o resto:
 *
 *   <form data-confirmar="Pergunta?">  pede confirmacao antes de enviar
 *   <select data-enviar-ao-mudar>      envia o formulario ao trocar a opcao
 *   <a data-voltar href="...">         volta para a pagina anterior
 *                                      (sem historico, segue o href)
 *
 * Os ouvintes ficam no documento inteiro, e nao em cada elemento: assim um
 * formulario novo so precisa do atributo para funcionar.
 */
(function () {

    document.addEventListener('submit', function (evento) {
        var pergunta = evento.target.getAttribute('data-confirmar');
        if (pergunta && !window.confirm(pergunta)) {
            evento.preventDefault();
        }
    });

    document.addEventListener('change', function (evento) {
        var campo = evento.target;
        if (campo.hasAttribute('data-enviar-ao-mudar') && campo.form) {
            campo.form.submit();
        }
    });

    document.addEventListener('click', function (evento) {
        var link = evento.target.closest('[data-voltar]');
        if (link && window.history.length > 1) {
            evento.preventDefault();
            window.history.back();
        }
    });
})();
