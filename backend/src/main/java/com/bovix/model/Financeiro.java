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
@Table(name = "financeiro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Financeiro {

    // Somente leitura no JSON: um id vindo do cliente faria o save() virar UPDATE de outro registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false)
    @JsonIgnore
    private Fazenda fazenda;

    @NotNull(message = "é obrigatória")
    @Column(nullable = false)
    private LocalDate data;

    @NotBlank(message = "é obrigatória")
    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(length = 255)
    private String descricao;

    @NotNull(message = "é obrigatório")
    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    @Transient
    @JsonProperty("fazendaId")
    public Long getFazendaId() {
        return fazenda != null ? fazenda.getId() : null;
    }
}
