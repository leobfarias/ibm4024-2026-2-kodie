package com.ibmec.kodie.config;

import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garante que exista um administrador para o primeiro acesso.
 *
 * So age quando a tabela de usuarios esta vazia: depois disso os acessos
 * sao gerenciados pela tela de usuarios. Login e senha vem das propriedades
 * kodie.admin.*, que podem ser sobrescritas por variaveis de ambiente -
 * no servidor, a senha padrao NUNCA deve ser usada.
 */
@Component
@Order(1)
public class UsuarioInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicial.class);
    private static final String SENHA_PADRAO = "kodie2026";

    private final UsuarioService usuarioService;
    private final String login;
    private final String nome;
    private final String senha;

    public UsuarioInicial(UsuarioService usuarioService,
                          @Value("${kodie.admin.login}") String login,
                          @Value("${kodie.admin.nome}") String nome,
                          @Value("${kodie.admin.senha}") String senha) {
        this.usuarioService = usuarioService;
        this.login = login;
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (usuarioService.existeAlgumUsuario()) {
            return;
        }

        usuarioService.criar(login, nome, senha, PerfilAcesso.ADMIN);
        log.info(">>> Administrador inicial criado: login '{}'", login);

        if (SENHA_PADRAO.equals(senha)) {
            log.warn(">>> O administrador foi criado com a SENHA PADRAO. Defina a variavel "
                    + "KODIE_ADMIN_SENHA antes de publicar o sistema, ou troque a senha "
                    + "pela tela 'Minha senha' no primeiro acesso.");
        }
    }
}
