package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_saude")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoSaude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    @JsonIgnore
    private Animal animal;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String produto;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "carencia_leite_dias")
    private Integer carenciaLeiteDias;

    @Column(name = "proxima_aplicacao_dias")
    private Integer proximaAplicacaoDias;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

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
