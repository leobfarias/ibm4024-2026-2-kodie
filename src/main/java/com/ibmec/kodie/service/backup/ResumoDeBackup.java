package com.ibmec.kodie.service.backup;

import java.util.List;

/**
 * Resultado da leitura de um backup: o conteudo e a lista de problemas que
 * impedem a restauracao. Lista vazia significa que o arquivo pode ser
 * restaurado com seguranca.
 */
public record ResumoDeBackup(String nomeDoArquivo, ArquivoDeBackup conteudo, List<String> problemas) {

    public boolean isRestauravel() {
        return conteudo != null && problemas.isEmpty();
    }

    public List<String> getAdministradoresAtivos() {
        if (conteudo == null || conteudo.usuarios() == null) {
            return List.of();
        }
        return conteudo.usuarios().stream()
                .filter(u -> u.ativo() && u.perfil() == com.ibmec.kodie.model.PerfilAcesso.ADMIN)
                .map(ArquivoDeBackup.UsuarioRegistro::login)
                .toList();
    }
}
