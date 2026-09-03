package com.bovix.repository;

import com.bovix.model.EventoReproducao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface EventoReproducaoRepository extends JpaRepository<EventoReproducao, Long> {
    List<EventoReproducao> findByAnimal_Fazenda_IdOrderByDataDesc(Long fazendaId);
    List<EventoReproducao> findByAnimal_IdAndTipoInOrderByDataDesc(Long animalId, Collection<String> tipos);
}
