package com.ibmec.kodie.config;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Encerra as sessoes abertas de um usuario.
 *
 * O perfil de quem entra pelo navegador fica guardado na sessao. Sem este
 * encerramento, um usuario desativado ou rebaixado continuaria operando com
 * o acesso antigo ate sair por conta propria.
 */
@Component
public class SessoesDeUsuario {

    private final SessionRegistry registro;

    public SessoesDeUsuario(SessionRegistry registro) {
        this.registro = registro;
    }

    /** Usado depois de uma restauracao: os usuarios passam a ser os do backup. */
    public void encerrarTodas() {
        registro.getAllPrincipals().stream()
                .flatMap(principal -> registro.getAllSessions(principal, false).stream())
                .forEach(sessao -> sessao.expireNow());
    }

    public void encerrar(String login) {
        registro.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UsuarioAutenticado usuario
                        && usuario.getUsername().equals(login))
                .flatMap(principal -> registro.getAllSessions(principal, false).stream())
                .forEach(sessao -> sessao.expireNow());
    }
}
