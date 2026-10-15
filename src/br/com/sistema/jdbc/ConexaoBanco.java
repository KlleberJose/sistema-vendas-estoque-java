/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.sistema.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author Klleber ADDJ
 */
public class ConexaoBanco {

    final private String url = DbConfig.url();
    final private String usuario = DbConfig.usuario();
    final private String senha = DbConfig.senha();

    public Connection pegarConexao() {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Erro ao se conectar com o banco de dados: " + e);
        }
        return null;
    }

}
