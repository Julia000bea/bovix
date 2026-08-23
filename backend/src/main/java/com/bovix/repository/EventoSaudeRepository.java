package com.bovix.repository;

import com.bovix.model.EventoSaude;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoSaudeRepository extends JpaRepository<EventoSaude, Long> {
    List<EventoSaude> findByAnimal_Fazenda_IdOrderByDataDesc(Long fazendaId);
}
