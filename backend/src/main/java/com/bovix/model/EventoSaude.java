package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_saude")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoSaude {

    // Somente leitura no JSON: um id vindo do cliente faria o save() virar UPDATE de outro registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    @JsonIgnore
    private Animal animal;

    @NotBlank(message = "é obrigatório")
    @Column(nullable = false, length = 20)
    private String tipo;

    @NotBlank(message = "é obrigatório")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String produto;

    @NotNull(message = "é obrigatória")
    @Column(nullable = false)
    private LocalDate data;

    @PositiveOrZero
    @Column(name = "carencia_leite_dias")
    private Integer carenciaLeiteDias;

    @Positive
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
