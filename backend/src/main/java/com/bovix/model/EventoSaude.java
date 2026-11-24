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

    // Tratamento de vários dias (ex.: 3 dias de antibiótico): a carência conta a partir da
    // última aplicação, que é data + (duração - 1). Vazio = dose única.
    @Min(value = 1, message = "deve ser de pelo menos 1 dia")
    @Max(value = 60, message = "deve ser de no máximo 60 dias")
    @Column(name = "duracao_tratamento_dias", nullable = false)
    private Integer duracaoTratamentoDias;

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
        if (duracaoTratamentoDias == null) duracaoTratamentoDias = 1;
    }

    /** Data da última aplicação do tratamento (igual a data, na dose única). */
    @Transient
    @JsonIgnore
    public LocalDate getDataUltimaAplicacao() {
        if (data == null) return null;
        int duracao = duracaoTratamentoDias != null && duracaoTratamentoDias > 1 ? duracaoTratamentoDias : 1;
        return data.plusDays(duracao - 1L);
    }

    /** Último dia em que o leite ainda está em carência (inclusive), ou null se não há carência. */
    @Transient
    @JsonProperty("fimCarencia")
    public LocalDate getFimCarencia() {
        if (data == null || carenciaLeiteDias == null || carenciaLeiteDias <= 0) return null;
        return getDataUltimaAplicacao().plusDays(carenciaLeiteDias);
    }

    @Transient
    @JsonProperty("animalId")
    public Long getAnimalId() {
        return animal != null ? animal.getId() : null;
    }
}
