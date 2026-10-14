package com.ibmec.kodie.repository;

import com.ibmec.kodie.model.PerfilAcesso;
import com.ibmec.kodie.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    /** Quantos administradores ativos restam - o sistema nunca pode ficar sem nenhum. */
    long countByPerfilAndAtivoTrue(PerfilAcesso perfil);

    List<Usuario> findAllByOrderByNomeAsc();
}
