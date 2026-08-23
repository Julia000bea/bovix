package com.bovix.repository;

import com.bovix.model.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {
    Optional<Configuracao> findByFazenda_Id(Long fazendaId);
    List<Configuracao> findByNotificacaoAtivaTrue();
}
