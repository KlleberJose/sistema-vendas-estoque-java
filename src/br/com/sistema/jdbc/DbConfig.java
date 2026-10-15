package br.com.sistema.jdbc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Le as configuracoes do banco de dados em tempo de execucao.
 * Ordem: arquivo db.properties (diretorio de execucao) -> variaveis de ambiente DB_URL/DB_USER/DB_PASSWORD.
 */
public final class DbConfig {

    private static final Properties PROPS = carregar();

    private DbConfig() {
    }

    private static Properties carregar() {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream("db.properties")) {
            p.load(in);
        } catch (IOException e) {
            // sem arquivo: usa variaveis de ambiente
        }
        return p;
    }

    private static String valor(String chave, String env, String padrao) {
        String v = PROPS.getProperty(chave);
        if (v == null) {
            v = System.getenv(env);
        }
        return v != null ? v : padrao;
    }

    public static String url() {
        return valor("db.url", "DB_URL", "jdbc:mysql://localhost:3306/sistemaestoque");
    }

    public static String usuario() {
        return valor("db.user", "DB_USER", "root");
    }

    public static String senha() {
        return valor("db.password", "DB_PASSWORD", "");
    }
}