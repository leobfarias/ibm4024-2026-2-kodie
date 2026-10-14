package com.ibmec.kodie.service.backup;

import java.time.LocalDateTime;

/** Um arquivo de backup guardado na pasta de backups do servidor. */
public record BackupSalvo(String nome, String motivo, long tamanhoBytes, LocalDateTime criadoEm) {

    public String getTamanhoLegivel() {
        if (tamanhoBytes < 1024) {
            return tamanhoBytes + " B";
        }
        if (tamanhoBytes < 1024 * 1024) {
            return String.format("%.1f KB", tamanhoBytes / 1024.0);
        }
        return String.format("%.1f MB", tamanhoBytes / (1024.0 * 1024.0));
    }

    public String getMotivoLegivel() {
        return switch (motivo) {
            case "automatico" -> "Automatico";
            case "manual" -> "Manual";
            case "antes-da-restauracao" -> "Antes de restaurar";
            case "enviado" -> "Enviado";
            default -> motivo;
        };
    }
}
