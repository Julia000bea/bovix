package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenhas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ordenha {

    // Somente leitura no JSON: um id vindo do cliente faria o save() virar UPDATE de outro registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false)
    @JsonIgnore
    private Fazenda fazenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    @JsonIgnore
    private Animal animal;

    @NotNull(message = "é obrigatória")
    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String periodo = "Dia todo";

    @NotNull(message = "é obrigatório")
    @PositiveOrZero
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal litros;

    /** true = volume dividido de uma coleta de vários dias, não medido neste dia. Definido pelo servidor. */
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    private Boolean estimado = false;

    /** Quantos dias a coleta cobre (1 = medição do próprio dia). A data informada é o último dia. */
    @Min(value = 1, message = "deve ser de pelo menos 1 dia")
    @Max(value = 31, message = "deve ser de no máximo 31 dias")
    @Column(name = "dias_coleta", nullable = false)
    @Builder.Default
    private Integer diasColeta = 1;

    /** Agrupa os registros gerados por uma mesma coleta de N dias. Definido pelo servidor. */
    @Column(name = "coleta_id", length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String coletaId;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (estimado == null) estimado = false;
        if (diasColeta == null) diasColeta = 1;
        if (periodo == null || periodo.isBlank()) periodo = "Dia todo";
    }

    @Transient
    @JsonProperty("fazendaId")
    public Long getFazendaId() {
        return fazenda != null ? fazenda.getId() : null;
    }

    @Transient
    @JsonProperty("animalId")
    public Long getAnimalId() {
        return animal != null ? animal.getId() : null;
    }
}
