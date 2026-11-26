package com.bovix.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteCadastrosTest {

    /** Relógio que o teste consegue adiantar. */
    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-11-26T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora.get(); }
    };
    private final LimiteCadastros limite = new LimiteCadastros(clock);

    @Test
    void bloqueiaDepoisDoMaximoELiberaDepoisDaJanela() {
        for (int i = 0; i < LimiteCadastros.MAX_POR_IP; i++) {
            limite.verificar("1.2.3.4");
            limite.registrar("1.2.3.4");
        }
        assertThatThrownBy(() -> limite.verificar("1.2.3.4")).isInstanceOf(LimiteCadastros.MuitosCadastrosException.class);
        assertThatCode(() -> limite.verificar("5.6.7.8")).doesNotThrowAnyException(); // outro IP não é afetado

        agora.set(agora.get().plus(LimiteCadastros.JANELA));
        assertThatCode(() -> limite.verificar("1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void usaOUltimoIpDoXForwardedFor() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        assertThat(LimiteCadastros.ipDoCliente(req)).isEqualTo("10.0.0.1");

        // o cliente pode inventar o primeiro valor; o último é o que o proxy do Render acrescentou
        req.addHeader("X-Forwarded-For", "9.9.9.9, 200.1.2.3");
        assertThat(LimiteCadastros.ipDoCliente(req)).isEqualTo("200.1.2.3");
    }
}
