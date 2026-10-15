package br.com.sistema.dao;

import br.com.sistema.jdbc.ConexaoBanco;
import br.com.sistema.model.Fornecedores;
import br.com.sistema.model.Produtos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class ProdutosDAO {

    private Connection conn;

    public ProdutosDAO() {
        this.conn = new ConexaoBanco().pegarConexao();
    }

    public void Salvar(Produtos obj) {
        try {
            String sql = "INSERT INTO tb_produtos (descricao, preco, qtd_estoque, for_id) "
                    + "VALUES (?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, obj.getDescricao());
            stmt.setDouble(2, obj.getPreco());
            stmt.setInt(3, obj.getQtdEstoque());
            stmt.setInt(4, obj.getFornecedores().getId());

            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Produto salvo com sucesso!");
        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao salvar o produto: " + erro);
        }
    }

    public void Editar(Produtos obj) {
        try {
            String sql = "UPDATE tb_produtos SET descricao = ?, preco = ?, qtd_estoque = ?, for_id = ? WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, obj.getDescricao());
            stmt.setDouble(2, obj.getPreco());
            stmt.setInt(3, obj.getQtdEstoque());
            stmt.setInt(4, obj.getFornecedores().getId());
            stmt.setInt(5, obj.getId());

            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Produto editado com sucesso!");
        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao editar os dados do produto: " + erro);
        }
    }

    public void Excluir(Produtos obj) {

        try {

            String sql = "DELETE FROM tb_produtos WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, obj.getId());
            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Produto excluido com sucesso!");

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao excluir produto: " + erro);
        }
    }

    public Produtos BuscarProdutos(String descricao) {
        try {
            String sql = "SELECT p.id, p.descricao, p.preco, p.qtd_estoque, f.nome FROM tb_produtos as p inner join tb_fornecedores as f on(p.for_id = f.id) WHERE p.descricao = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, descricao);

            ResultSet rs = stmt.executeQuery();
            Produtos obj = new Produtos();

            Fornecedores f = new Fornecedores();

            if (rs.next()) {
                obj.setId(rs.getInt("p.id"));
                obj.setDescricao(rs.getString("p.descricao"));
                obj.setPreco(rs.getDouble("p.preco"));
                obj.setQtdEstoque(rs.getInt("p.qtd_estoque"));

                f.setNome(rs.getString("f.nome"));
                obj.setFornecedores(f);
            }

            rs.close();
            stmt.close();
            return obj;

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao buscar produto: " + erro);
            return null;
        }
    }

    public Produtos BuscarProdutosCodigo(int id) {
        try {
            String sql = "SELECT p.id, p.descricao, p.preco, p.qtd_estoque, f.nome FROM tb_produtos as p inner join tb_fornecedores as f on(p.for_id = f.id) WHERE p.id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            Produtos obj = new Produtos();

            Fornecedores f = new Fornecedores();

            if (rs.next()) {
                obj.setId(rs.getInt("p.id"));
                obj.setDescricao(rs.getString("p.descricao"));
                obj.setPreco(rs.getDouble("p.preco"));
                obj.setQtdEstoque(rs.getInt("p.qtd_estoque"));

                f.setNome(rs.getString("f.nome"));
                obj.setFornecedores(f);
            }

            rs.close();
            stmt.close();
            return obj;

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao buscar produto: " + erro);
            return null;
        }
    }

    public List<Produtos> Listar() {
        List<Produtos> lista = new ArrayList<>();

        try {
            String sql = "SELECT p.id, p.descricao, p.preco, p.qtd_estoque, f.nome FROM tb_produtos as p inner join tb_fornecedores as f on(p.for_id = f.id)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Produtos obj = new Produtos();
                Fornecedores f = new Fornecedores();

                obj.setId(rs.getInt("p.id"));
                obj.setDescricao(rs.getString("p.descricao"));
                obj.setPreco(rs.getDouble("p.preco"));
                obj.setQtdEstoque(rs.getInt("p.qtd_estoque"));
                obj.setFornecedores(f);
                f.setNome(rs.getString("f.nome"));

                lista.add(obj);
            }

            rs.close();
            stmt.close();

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao criar a lista: " + erro);
        }
        return lista;
    }

    public List<Produtos> Filtrar(String descricao) {
        List<Produtos> lista = new ArrayList<>();

        try {
            String sql = "SELECT p.id, p.descricao, p.preco, p.qtd_estoque, f.nome FROM tb_produtos as p inner join tb_fornecedores as f on(p.for_id = f.id) WHERE p.descricao like ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, descricao);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Produtos obj = new Produtos();
                Fornecedores f = new Fornecedores();

                obj.setId(rs.getInt("p.id"));
                obj.setDescricao(rs.getString("p.descricao"));
                obj.setPreco(rs.getDouble("p.preco"));
                obj.setQtdEstoque(rs.getInt("p.qtd_estoque"));

                f.setNome(rs.getString("f.nome"));
                obj.setFornecedores(f);

                lista.add(obj);
            }

            rs.close();
            stmt.close();

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao criar a lista: " + erro);
        }
        return lista;
    }

    public void adicionarEstoque(int id, int quantidadeNova) {
        try {

            String sql = "UPDATE tb_produtos SET qtd_estoque = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, quantidadeNova);
            stmt.setInt(2, id);

            stmt.executeUpdate();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Adicionado com sucesso ao estoque");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Erro ao adicionar ao estoque: " + e);
        }
    }

    public void baixaEstoque(int id, int quantidadeNova) {

        try {

            String sql = "UPDATE tb_produtos set qtd_estoque = ? where id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, quantidadeNova);
            stmt.setInt(2, id);

            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Baixa no estoque efetuada com sucesso.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao dar baixa no estoque: " + e);
        }
    }

    public int retornaQuantidadeAtualEstoque(int id) {

        try {

            int quantidadeAtualEstoque = 0;
            String sql = "SELECT qtd_estoque FROM tb_produtos WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                quantidadeAtualEstoque = (rs.getInt("qtd_estoque"));
            }

            return quantidadeAtualEstoque;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao retornar a quantidade atual do estoque: " + e);
        }
    }
}
