package com.bovix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "configuracoes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Configuracao {

    // Somente leitura no JSON: um id vindo do cliente faria o save() virar UPDATE de outro registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false, unique = true)
    @JsonIgnore
    private Fazenda fazenda;

    @Column(name = "gestacao_dias", nullable = false)
    @Builder.Default
    private Integer gestacaoDias = 283;

    @Column(name = "cio_dias", nullable = false)
    @Builder.Default
    private Integer cioDias = 21;

    /** "Vacas vazias há mais de X dias após o parto" nos indicadores reprodutivos. */
    @Column(name = "dias_vazia_alerta", nullable = false)
    @Builder.Default
    private Integer diasVaziaAlerta = 90;

    /** Dias antes do parto previsto em que a vaca é secada (alerta de secagem e projeção). */
    @Column(name = "periodo_seco_dias", nullable = false)
    @Builder.Default
    private Integer periodoSecoDias = 60;

    // --- Relatório diário por e-mail ---
    @Column(name = "notificacao_ativa", nullable = false)
    @Builder.Default
    private Boolean notificacaoAtiva = false;

    @Column(name = "notificacao_email", length = 180)
    private String notificacaoEmail;

    @Column(name = "notificacao_horario", nullable = false)
    @Builder.Default
    private LocalTime notificacaoHorario = LocalTime.of(7, 0);

    // Controla se já mandou hoje, pra não duplicar quando o job roda de novo no mesmo dia.
    @JsonIgnore
    @Column(name = "notificacao_ultimo_envio")
    private LocalDate notificacaoUltimoEnvio;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
    }

    // Expõe só o id (seguro mesmo com proxy não inicializado — não bate no banco).
    @Transient
    @JsonProperty("fazendaId")
    public Long getFazendaId() {
        return fazenda != null ? fazenda.getId() : null;
    }
}
