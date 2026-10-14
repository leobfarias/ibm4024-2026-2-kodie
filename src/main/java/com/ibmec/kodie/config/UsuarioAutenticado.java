package com.ibmec.kodie.config;

import com.ibmec.kodie.model.Avatar;
import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * O usuario logado, como o Spring Security o enxerga.
 *
 * Alem do login e das permissoes, guarda o nome e o perfil para que as telas
 * possam exibi-los sem consultar o banco a cada requisicao.
 */
public class UsuarioAutenticado extends User {

    private final String nome;
    private final PerfilAcesso perfil;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getLogin(),
              usuario.getSenhaHash(),
              usuario.isAtivo(),            // habilitado
              true, true, true,             // conta, credencial e bloqueio: nao expiram
              List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
        this.nome = usuario.getNome();
        this.perfil = usuario.getPerfil();
    }

    public String getNome() {
        return nome;
    }

    public PerfilAcesso getPerfil() {
        return perfil;
    }

    public boolean isAdmin() {
        return perfil == PerfilAcesso.ADMIN;
    }

    public String getIniciais() {
        return Avatar.iniciais(nome);
    }

    public String getCorAvatar() {
        return Avatar.cor(nome);
    }
}
