package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fato do dia que pode explicar uma variação na produção (água, alimentação, equipamento...). */
@Entity
@Table(name = "ocorrencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ocorrencia {

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

    @NotBlank(message = "é obrigatório")
    @Column(nullable = false, length = 20)
    private String tipo;

    @NotBlank(message = "é obrigatória")
    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "criado_em", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
