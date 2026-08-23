package com.bovix.repository;

import com.bovix.model.Financeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface FinanceiroRepository extends JpaRepository<Financeiro, Long> {
    List<Financeiro> findByFazenda_IdAndDataBetweenOrderByDataDesc(Long fazendaId, LocalDate inicio, LocalDate fim);
}
