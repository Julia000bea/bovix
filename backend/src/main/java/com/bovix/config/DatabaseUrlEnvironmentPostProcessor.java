package com.bovix.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aceita a string de conexão no formato que o Neon/Render costumam fornecer
 * (postgresql://usuario:senha@host/banco?sslmode=require&channel_binding=require) e a converte
 * para o formato que o driver JDBC entende:
 * spring.datasource.url = jdbc:postgresql://host/banco?sslmode=require, mais username/password separados.
 *
 * Fontes, em ordem: spring.datasource.url (SPRING_DATASOURCE_URL) com credenciais embutidas ou sem o
 * prefixo jdbc:, ou DATABASE_URL quando SPRING_DATASOURCE_URL não estiver definida.
 * URLs já no formato JDBC sem credenciais não são tocadas.
 *
 * Nunca registra a URL nem a senha em log — as mensagens de erro também não as incluem.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String NOME_FONTE = "bovixDatasourceConvertida";

    // parâmetros da libpq que o pgjdbc não reconhece
    private static final Set<String> PARAMETROS_SO_LIBPQ = Set.of("channel_binding");

    private final Log log;

    public DatabaseUrlEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(DatabaseUrlEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String origem = "spring.datasource.url";
        String url = environment.getProperty("spring.datasource.url");

        if (!precisaConverter(url)) {
            String databaseUrl = environment.getProperty("DATABASE_URL");
            boolean temUrlSpring = isPreenchida(environment.getProperty("SPRING_DATASOURCE_URL"));
            if (temUrlSpring || !isPreenchida(databaseUrl)) return;
            origem = "DATABASE_URL";
            url = databaseUrl;
        }

        ConexaoJdbc conexao = converter(url, origem);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", conexao.url());
        if (conexao.usuario() != null) props.put("spring.datasource.username", conexao.usuario());
        if (conexao.senha() != null) props.put("spring.datasource.password", conexao.senha());
        environment.getPropertySources().addFirst(new MapPropertySource(NOME_FONTE, props));

        log.info("Conexão do banco convertida a partir de " + origem + " para o formato JDBC"
                + (conexao.usuario() != null ? " (usuário e senha extraídos da URL)" : ""));
    }

    /** Converte apenas URLs do Postgres que o driver JDBC rejeitaria. */
    static boolean precisaConverter(String url) {
        if (!isPreenchida(url)) return false;
        String u = url.trim();
        if (u.startsWith("postgres://") || u.startsWith("postgresql://")) return true;
        if (!u.startsWith("jdbc:postgresql://")) return false;
        String semPrefixo = u.substring("jdbc:postgresql://".length());
        int fimAutoridade = indiceFimAutoridade(semPrefixo);
        boolean temCredenciais = semPrefixo.substring(0, fimAutoridade).contains("@");
        boolean temParametroLibpq = PARAMETROS_SO_LIBPQ.stream().anyMatch(p -> u.contains(p + "="));
        return temCredenciais || temParametroLibpq;
    }

    static ConexaoJdbc converter(String original, String origem) {
        String u = original.trim();
        if (u.startsWith("jdbc:")) u = u.substring("jdbc:".length());
        if (u.startsWith("postgres://")) u = "postgresql://" + u.substring("postgres://".length());

        URI uri;
        try {
            uri = new URI(u);
        } catch (Exception e) {
            // não repassa a mensagem original: ela contém a URL completa, com a senha
            throw new IllegalStateException(origem + " não é uma URL PostgreSQL válida. Use "
                    + "jdbc:postgresql://<host>/<banco>?sslmode=require e informe usuário e senha em "
                    + "SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD (caracteres especiais na senha "
                    + "precisam estar codificados, ex.: @ como %40).");
        }
        if (uri.getHost() == null) {
            throw new IllegalStateException(origem + " não tem um host válido. Formato esperado: "
                    + "jdbc:postgresql://<host>/<banco>?sslmode=require");
        }

        String usuario = null;
        String senha = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            int separador = userInfo.indexOf(':');
            usuario = decodificar(separador < 0 ? userInfo : userInfo.substring(0, separador));
            senha = separador < 0 ? null : decodificar(userInfo.substring(separador + 1));
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() > 0) jdbc.append(':').append(uri.getPort());
        if (uri.getRawPath() != null) jdbc.append(uri.getRawPath());
        String parametros = filtrarParametros(uri.getRawQuery());
        if (!parametros.isEmpty()) jdbc.append('?').append(parametros);

        return new ConexaoJdbc(jdbc.toString(), usuario, senha);
    }

    private static String filtrarParametros(String query) {
        if (query == null || query.isEmpty()) return "";
        return Arrays.stream(query.split("&"))
                .filter(p -> !p.isEmpty())
                .filter(p -> !PARAMETROS_SO_LIBPQ.contains(p.split("=", 2)[0]))
                .collect(Collectors.joining("&"));
    }

    private static int indiceFimAutoridade(String semEsquema) {
        int fim = semEsquema.length();
        for (char c : new char[]{'/', '?'}) {
            int i = semEsquema.indexOf(c);
            if (i >= 0 && i < fim) fim = i;
        }
        return fim;
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static boolean isPreenchida(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public int getOrder() {
        // depois do carregamento do application.yml e das variáveis de ambiente
        return Ordered.LOWEST_PRECEDENCE;
    }

    record ConexaoJdbc(String url, String usuario, String senha) {
        @Override
        public String toString() {
            return "ConexaoJdbc[url=" + url + ", usuario=" + usuario + ", senha=****]";
        }
    }
}
