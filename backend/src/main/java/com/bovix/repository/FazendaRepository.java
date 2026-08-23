package com.bovix.repository;

import com.bovix.model.Fazenda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FazendaRepository extends JpaRepository<Fazenda, Long> {
    List<Fazenda> findByUsuario_Id(Long usuarioId);
}
