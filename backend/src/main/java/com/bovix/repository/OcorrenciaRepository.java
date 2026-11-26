package com.bovix.repository;

import com.bovix.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {
    List<Ocorrencia> findByFazenda_IdAndDataBetweenOrderByDataDesc(Long fazendaId, LocalDate inicio, LocalDate fim);
}
