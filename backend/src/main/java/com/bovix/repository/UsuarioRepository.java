package com.bovix.repository;

import com.bovix.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    // contas antigas podem ter sido criadas com maiúsculas no e-mail
    Optional<Usuario> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);
}
