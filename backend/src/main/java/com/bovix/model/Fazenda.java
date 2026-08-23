package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fazendas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Fazenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nunca serializar o usuário aqui (evita expor senhaHash e evita LazyInitializationException).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nome;

    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
