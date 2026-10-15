/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.sistema.dao;

import br.com.sistema.jdbc.ConexaoBanco;
import br.com.sistema.model.ItensVendas;
import br.com.sistema.model.Produtos;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Klleber ADDJ
 */
public class ItensVendasDAO {

    private Connection conn;

    public ItensVendasDAO() {
        this.conn = new ConexaoBanco().pegarConexao();
    }

    public void salvar(ItensVendas obj) {

        try {

            String sql = "INSERT INTO tb_itensvendas (venda_id, produto_id, qtd, subtotal) values (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, obj.getVendas().getId());
            stmt.setInt(2, obj.getProdutos().getId());
            stmt.setInt(3, obj.getQuantidade());
            stmt.setDouble(4, obj.getSubtotal());

            stmt.execute();
            stmt.close();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar os itens da venda!");
        }
    }

    public List<ItensVendas> listaItens(int vendaId) {
        try {
            List<ItensVendas> lista = new ArrayList<>();
            String sql = "SELECT p.id, p.descricao, i.qtd, p.preco, i.subtotal"
                    + " FROM tb_itensvendas AS i INNER JOIN tb_produtos"
                    + " AS p ON (i.produto_id = p.id) WHERE i.venda_id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, vendaId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ItensVendas item = new ItensVendas();
                Produtos produto = new Produtos();

                produto.setId(rs.getInt("p.id"));
                item.setProdutos(produto);
                produto.setDescricao(rs.getString("p.descricao"));
                item.setProdutos(produto);
                item.setQuantidade(rs.getInt("i.qtd"));
                produto.setPreco(rs.getDouble("p.preco"));
                item.setProdutos(produto);
                item.setSubtotal(rs.getInt("i.subtotal"));

                lista.add(item);
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cria a lista de itens: " + e);
        }
    }
}
