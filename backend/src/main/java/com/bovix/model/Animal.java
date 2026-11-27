package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "animais", uniqueConstraints = @UniqueConstraint(columnNames = {"fazenda_id", "brinco"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Animal {

    public static final Set<String> CATEGORIAS_FEMEA = Set.of("Bezerra", "Novilha", "Vaca lactante", "Vaca seca");
    public static final Set<String> CATEGORIAS_MACHO = Set.of("Bezerro", "Touro");

    /** Bezerro ou touro: não tem eventos de reprodução nem ordenha. */
    public boolean ehMacho() {
        return CATEGORIAS_MACHO.contains(categoria);
    }

    // Somente leitura no JSON: um id vindo do cliente faria o save() virar UPDATE de outro registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false)
    @JsonIgnore
    private Fazenda fazenda;

    @NotBlank(message = "é obrigatório")
    @Size(max = 30)
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

    /** Dia da venda ou da morte. Preenchido pelo servidor ao mudar o status (ou informado pelo cliente). */
    @Column(name = "data_saida")
    private LocalDate dataSaida;

    // Definida só pelo upload (AnimalService.salvarFoto), nunca pelo JSON do cliente.
    @Column(name = "foto_url", length = 500)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
