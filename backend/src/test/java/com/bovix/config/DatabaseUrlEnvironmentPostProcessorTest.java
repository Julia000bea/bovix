package com.bovix.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Credenciais abaixo são fictícias. */
class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor =
            new DatabaseUrlEnvironmentPostProcessor(supplier -> supplier.get());

    /** Simula o application.yml real + variáveis de ambiente do sistema. */
    private StandardEnvironment ambiente(Map<String, Object> variaveis) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("variaveis", variaveis));
        env.getPropertySources().addLast(new MapPropertySource("application.yml", Map.of(
                "spring.datasource.url", "${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/bovix}",
                "spring.datasource.username", "${SPRING_DATASOURCE_USERNAME:bovix}",
                "spring.datasource.password", "${SPRING_DATASOURCE_PASSWORD:bovix}")));
        return env;
    }

    private StandardEnvironment processar(Map<String, Object> variaveis) {
        StandardEnvironment env = ambiente(variaveis);
        processor.postProcessEnvironment(env, null);
        return env;
    }

    @Test
    void urlDoNeonComCredenciaisEChannelBindingEhConvertida() {
        var env = processar(Map.of("SPRING_DATASOURCE_URL",
                "jdbc:postgresql://neondb_owner:segredo123@ep-x-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"));

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-x-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("neondb_owner");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("segredo123");
    }

    @Test
    void formatoLibpqSemPrefixoJdbcEhAceito() {
        var env = processar(Map.of("SPRING_DATASOURCE_URL",
                "postgresql://usuario:senha@host.neon.tech/neondb?sslmode=require"));

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host.neon.tech/neondb?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("usuario");
    }

    @Test
    void databaseUrlEhUsadaQuandoNaoHaSpringDatasourceUrl() {
        var env = processar(Map.of("DATABASE_URL", "postgres://u:p@db.example.com:6543/app"));

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db.example.com:6543/app");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("u");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("p");
    }

    @Test
    void springDatasourceUrlTemPrioridadeSobreDatabaseUrl() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://certo.neon.tech/neondb?sslmode=require");
        vars.put("SPRING_DATASOURCE_USERNAME", "usuario_certo");
        vars.put("DATABASE_URL", "postgres://outro:x@errado.example.com/db");
        var env = processar(vars);

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://certo.neon.tech/neondb?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("usuario_certo");
        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.NOME_FONTE)).isFalse();
    }

    @Test
    void urlJdbcCorretaNaoEhAlterada() {
        var env = processar(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://host.neon.tech/neondb?sslmode=require",
                "SPRING_DATASOURCE_USERNAME", "usuario",
                "SPRING_DATASOURCE_PASSWORD", "senha"));

        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.NOME_FONTE)).isFalse();
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("senha");
    }

    @Test
    void semVariaveisUsaOPadraoLocal() {
        var env = processar(Map.of());

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5433/bovix");
    }

    @Test
    void senhaComCaracteresCodificadosEhDecodificada() {
        var env = processar(Map.of("DATABASE_URL", "postgresql://u:p%40ss%3Aw%2Frd@h.example.com/db"));

        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("p@ss:w/rd");
    }

    @Test
    void erroDeUrlInvalidaNaoExpoeASenha() {
        assertThatThrownBy(() -> processar(Map.of("SPRING_DATASOURCE_URL",
                "postgresql://u:senha secreta@host/db")))
                .isInstanceOf(IllegalStateException.class)
                .message().doesNotContain("senha secreta");
    }

    @Test
    void toStringNaoExpoeASenha() {
        var conexao = DatabaseUrlEnvironmentPostProcessor.converter("postgresql://u:segredo@h/db", "teste");
        assertThat(conexao.toString()).doesNotContain("segredo");
    }
}
