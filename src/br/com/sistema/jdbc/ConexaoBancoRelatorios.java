/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.sistema.jdbc;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author Klleber ADDJ
 */
public class ConexaoBancoRelatorios {

    final private String driver = "com.mysql.cj.jdbc.Driver";
    final private String url = DbConfig.url();
    final private String usuario = DbConfig.usuario();
    final private String senha = DbConfig.senha();
    private Connection conexao;
    public Statement statement;
    public ResultSet resultSet;

    public boolean conecta() {

        boolean resultado = true;

        try {
            Class.forName(driver);
            conexao = DriverManager.getConnection(url, usuario, senha);
        } catch (ClassNotFoundException Driver) {
            JOptionPane.showMessageDialog(null, "Driver não encontrado: " + Driver);
            resultado = false;
        } catch (SQLException fonte) {
            JOptionPane.showMessageDialog(null, "Erro na fonte de dados: " + fonte.getMessage());
            resultado = false;
        }

        return resultado;
    }

    public void desconecta() {
        boolean resultado = true;

        try {
            conexao.close();
            JOptionPane.showMessageDialog(null, "Banco fechado!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao desconectar com o banco de dados");
            resultado = false;
        }
    }

    public void executeSQL(String sql) {

        try {
            statement = conexao.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            resultSet = statement.executeQuery(sql);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro de sql: " + e.getMessage());
        }
    }

}
