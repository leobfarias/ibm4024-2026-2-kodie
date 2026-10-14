package com.ibmec.kodie.service.indicadores;

/**
 * Uma linha de um grafico de barras: rotulo, quantidade e a fracao do total.
 *
 * @param percentual 0 a 100, ja arredondado, usado como largura da barra
 * @param cor        classe CSS da barra
 */
public record Fatia(String rotulo, long quantidade, int percentual, String cor) {

    public static Fatia de(String rotulo, long quantidade, long total, String cor) {
        int percentual = total == 0 ? 0 : (int) Math.round(quantidade * 100.0 / total);
        return new Fatia(rotulo, quantidade, percentual, cor);
    }
}
