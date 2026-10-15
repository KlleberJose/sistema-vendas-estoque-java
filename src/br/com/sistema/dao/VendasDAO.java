/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.sistema.dao;

import br.com.sistema.jdbc.ConexaoBanco;
import br.com.sistema.model.Clientes;
import br.com.sistema.model.Vendas;
import java.awt.HeadlessException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author Klleber ADDJ
 */
public class VendasDAO {

    private final Connection conn;

    public VendasDAO() {
        this.conn = new ConexaoBanco().pegarConexao();
    }

    public void salvar(Vendas obj) {

        try {
            String sql = "INSERT INTO tb_vendas (cliente_id, data_venda, total_venda, observacoes) VALUES(?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, obj.getClientes().getId());
                stmt.setString(2, obj.getDataVenda());
                stmt.setDouble(3, obj.getTotalDaVenda());
                stmt.setString(4, obj.getObservacoes());

                stmt.execute();
            }
            JOptionPane.showMessageDialog(null, "Venda realizada com sucesso.");

        } catch (HeadlessException | SQLException e) {
            throw new RuntimeException("Erro ao realizar uma venda: " + e);
        }
    }

    public int retornaUltimoIdVenda() {

        try {
            int ultimoId = 0;
            String sql = "SELECT max(id) id from tb_vendas";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Vendas v = new Vendas();
                v.setId(rs.getInt("id"));
                ultimoId = v.getId();
            }

            return ultimoId;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao retornar o ultimo id da venda: " + e);
        }
    }

    public List<Vendas> historicoVendas(LocalDate dataInicio, LocalDate dataFim) {

        try {

            List<Vendas> lista = new ArrayList<>();

            String sql = "SELECT v.id, c.nome, "
                    + "date_format(v.data_venda, '%d/%m/%Y') AS data_formatada, "
                    + "v.total_venda, v.observacoes "
                    + "FROM tb_vendas AS v "
                    + "INNER JOIN tb_clientes AS c ON(v.cliente_id = c.id) "
                    + "WHERE v.data_venda BETWEEN ? AND ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, dataInicio.toString());
            stmt.setString(2, dataFim.toString());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                Vendas v = new Vendas();
                Clientes c = new Clientes();

                v.setId(rs.getInt("v.id"));
                c.setNome(rs.getString("c.nome"));
                v.setClientes(c);
                v.setDataVenda(rs.getString("data_formatada"));
                v.setTotalDaVenda(rs.getDouble("v.total_venda"));
                v.setObservacoes(rs.getString("v.observacoes"));
                lista.add(v);
            }

            rs.close();
            stmt.close();

            return lista;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao criar o histórico de vendas: " + e
            );
        }
    }

    public double posicaoDoDia(LocalDate dataVenda) {
        try {

            double totalDoDia = 0;
            String sql = "SELECT sum(total_venda) AS total FROM tb_vendas WHERE data_venda = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dataVenda.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                totalDoDia = rs.getDouble("total");
            }
            return totalDoDia;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao retornar a posição do dia: " + e);
        }
    }
}
