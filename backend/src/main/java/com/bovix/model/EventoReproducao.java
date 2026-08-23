package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_reproducao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoReproducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    @JsonIgnore
    private Animal animal;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 255)
    private String detalhe;

    @Column(length = 20)
    private String resultado;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    @Transient
    @JsonProperty("animalId")
    public Long getAnimalId() {
        return animal != null ? animal.getId() : null;
    }
}
