package com.bovix.repository;

import com.bovix.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    List<Animal> findByFazenda_IdOrderByBrincoAsc(Long fazendaId);
    boolean existsByIdAndFazenda_Usuario_Id(Long id, Long usuarioId);
}
