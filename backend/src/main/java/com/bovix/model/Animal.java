package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "animais", uniqueConstraints = @UniqueConstraint(columnNames = {"fazenda_id", "brinco"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false)
    @JsonIgnore
    private Fazenda fazenda;

    @Column(nullable = false, length = 30)
    private String brinco;

    @Column(length = 120)
    private String nome;

    @Column(length = 80)
    private String raca;

    private LocalDate nascimento;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String categoria = "Bezerra";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "Ativa";

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    @Transient
    @JsonProperty("fazendaId")
    public Long getFazendaId() {
        return fazenda != null ? fazenda.getId() : null;
    }
}
