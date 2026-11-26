package com.bovix.repository;

import com.bovix.model.Ordenha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface OrdenhaRepository extends JpaRepository<Ordenha, Long> {
    List<Ordenha> findByFazenda_IdAndDataBetweenOrderByDataDesc(Long fazendaId, LocalDate inicio, LocalDate fim);

    boolean existsByFazenda_IdAndDataAndAnimalIsNull(Long fazendaId, LocalDate data);

    boolean existsByFazenda_IdAndDataAndAnimalIsNotNull(Long fazendaId, LocalDate data);

    List<Ordenha> findByFazenda_IdAndColetaId(Long fazendaId, String coletaId);
}
