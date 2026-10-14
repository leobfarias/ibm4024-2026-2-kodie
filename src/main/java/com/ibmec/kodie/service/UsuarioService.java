package com.ibmec.kodie.service;

import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.model.Usuario;
import com.ibmec.kodie.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    public static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<Usuario> buscarPorLogin(String login) {
        return repository.findByLogin(login);
    }

    public boolean existeAlgumUsuario() {
        return repository.count() > 0;
    }

    /**
     * Regra de negocio com consulta ao banco: o login identifica a pessoa
     * no acesso ao sistema, entao nao pode se repetir.
     */
    public Usuario criar(String login, String nome, String senha, PerfilAcesso perfil) {
        String loginNormalizado = login == null ? null : login.trim().toLowerCase();

        if (repository.existsByLogin(loginNormalizado)) {
            throw new IllegalArgumentException("Ja existe um usuario com o login " + loginNormalizado);
        }
        validarSenha(senha);

        Usuario usuario = new Usuario(loginNormalizado, nome.trim(), passwordEncoder.encode(senha), perfil);
        return repository.save(usuario);
    }

    /**
     * Ninguem altera o proprio perfil: um administrador que se rebaixasse
     * perderia o acesso a esta mesma tela no meio da operacao.
     */
    @Transactional
    public void alterarPerfil(Long id, PerfilAcesso novoPerfil, String loginDeQuemAltera) {
        Usuario usuario = encontrar(id);

        if (usuario.getLogin().equals(loginDeQuemAltera)) {
            throw new IllegalArgumentException("Voce nao pode alterar o seu proprio perfil de acesso");
        }
        if (usuario.isAdmin() && novoPerfil != PerfilAcesso.ADMIN && usuario.isAtivo()) {
            garantirQueSobraOutroAdmin();
        }

        usuario.setPerfil(novoPerfil);
        repository.save(usuario);
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo, String loginDeQuemAltera) {
        Usuario usuario = encontrar(id);

        if (!ativo && usuario.getLogin().equals(loginDeQuemAltera)) {
            throw new IllegalArgumentException("Voce nao pode desativar o seu proprio usuario");
        }
        if (!ativo && usuario.isAdmin() && usuario.isAtivo()) {
            garantirQueSobraOutroAdmin();
        }

        usuario.setAtivo(ativo);
        repository.save(usuario);
    }

    /** Usada pelo administrador para definir uma nova senha para outro usuario. */
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = encontrar(id);
        validarSenha(novaSenha);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        repository.save(usuario);
    }

    /** Usada por qualquer usuario para trocar a propria senha, mediante a senha atual. */
    @Transactional
    public void trocarPropriaSenha(String login, String senhaAtual, String novaSenha) {
        Usuario usuario = repository.findByLogin(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + login));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("A senha atual nao confere");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual");
        }
        validarSenha(novaSenha);

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        repository.save(usuario);
    }

    /**
     * Regra de negocio com consulta ao banco: o sistema nunca pode ficar sem
     * nenhum administrador ativo, senao ninguem mais conseguiria gerenciar
     * os acessos.
     */
    private void garantirQueSobraOutroAdmin() {
        if (repository.countByPerfilAndAtivoTrue(PerfilAcesso.ADMIN) <= 1) {
            throw new IllegalArgumentException("O sistema precisa de pelo menos um administrador ativo");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
        }
    }

    private Usuario encontrar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
    }
}
