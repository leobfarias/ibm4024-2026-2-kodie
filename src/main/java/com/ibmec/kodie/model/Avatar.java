package com.ibmec.kodie.model;

/**
 * Iniciais e cor do circulo de identificacao exibido nas telas.
 *
 * A cor e derivada do nome, entao a mesma pessoa aparece sempre com a mesma
 * cor, em qualquer tela, sem nada armazenado no banco.
 */
public final class Avatar {

    private static final int QUANTIDADE_DE_CORES = 8;

    private Avatar() {
    }

    /** "Ana Beatriz Moreira" -> "AM" */
    public static String iniciais(String nome) {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase();
    }

    /** Classe CSS da cor: c0 a c7. */
    public static String cor(String nome) {
        if (nome == null) {
            return "c0";
        }
        return "c" + Math.floorMod(nome.trim().toLowerCase().hashCode(), QUANTIDADE_DE_CORES);
    }
}
