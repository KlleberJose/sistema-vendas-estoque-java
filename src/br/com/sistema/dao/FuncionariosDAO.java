package br.com.sistema.dao;

import br.com.sistema.jdbc.ConexaoBanco;
import br.com.sistema.model.Funcionarios;
import br.com.sistema.view.AreaTrabalho;
import br.com.sistema.view.FormularioLogin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class FuncionariosDAO {

    private Connection conn;

    public FuncionariosDAO() {
        this.conn = new ConexaoBanco().pegarConexao();
    }

    public void Salvar(Funcionarios obj) {
        try {
            String sql = "INSERT INTO tb_funcionarios (nome, rg, cpf, email, senha, cargo, nivel_acesso, telefone, celular, cep, "
                    + "endereco, numero, complemento, bairro, cidade, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, obj.getNome());
            stmt.setString(2, obj.getRg());
            stmt.setString(3, obj.getCpf());
            stmt.setString(4, obj.getEmail());
            stmt.setString(5, obj.getSenha());
            stmt.setString(6, obj.getCargo());
            stmt.setString(7, obj.getNivel_acesso());
            stmt.setString(8, obj.getTelefone());
            stmt.setString(9, obj.getCelular());
            stmt.setString(10, obj.getCep());
            stmt.setString(11, obj.getEndereco());
            stmt.setInt(12, obj.getNumero());
            stmt.setString(13, obj.getComplemento());
            stmt.setString(14, obj.getBairro());
            stmt.setString(15, obj.getCidade());
            stmt.setString(16, obj.getEstado());

            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Funcionário salvo com sucesso!");
        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao salvar o funcionário: " + erro);
        }
    }

    public void Editar(Funcionarios obj) {
        try {
            String sql = "UPDATE tb_funcionarios SET nome = ?, rg = ?, cpf = ?, email = ?, senha = ?, cargo = ?, nivel_acesso = ?, telefone = ?, celular = ?, cep = ?,"
                    + "endereco = ?, numero = ?, complemento = ?, bairro = ?, cidade = ?, estado = ? WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, obj.getNome());
            stmt.setString(2, obj.getRg());
            stmt.setString(3, obj.getCpf());
            stmt.setString(4, obj.getEmail());
            stmt.setString(5, obj.getSenha());
            stmt.setString(6, obj.getCargo());
            stmt.setString(7, obj.getNivel_acesso());
            stmt.setString(8, obj.getTelefone());
            stmt.setString(9, obj.getCelular());
            stmt.setString(10, obj.getCep());
            stmt.setString(11, obj.getEndereco());
            stmt.setInt(12, obj.getNumero());
            stmt.setString(13, obj.getComplemento());
            stmt.setString(14, obj.getBairro());
            stmt.setString(15, obj.getCidade());
            stmt.setString(16, obj.getEstado());
            stmt.setInt(17, obj.getId());

            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Cadastro do funcionário editado com sucesso!");
        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao editar os dados do funcionário: " + erro);
        }
    }

    public void Excluir(Funcionarios obj) {

        try {

            String sql = "DELETE FROM tb_funcionarios WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, obj.getId());
            stmt.execute();
            stmt.close();

            JOptionPane.showMessageDialog(null, "Funcionário excluido com sucesso!");

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao excluir Funcionário: " + erro);
        }
    }

    public Funcionarios BuscarFuncionarios(String nome) {
        try {
            String sql = "SELECT * FROM tb_funcionarios WHERE nome = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nome);

            ResultSet rs = stmt.executeQuery();
            Funcionarios obj = new Funcionarios();

            if (rs.next()) {
                obj.setId(rs.getInt("id"));
                obj.setNome(rs.getString("nome"));
                obj.setRg(rs.getString("rg"));
                obj.setCpf(rs.getString("cpf"));
                obj.setEmail(rs.getString("email"));
                obj.setSenha(rs.getString("senha"));
                obj.setCargo(rs.getString("cargo"));
                obj.setNivel_acesso(rs.getString("nivel_acesso"));
                obj.setTelefone(rs.getString("telefone"));
                obj.setCelular(rs.getString("celular"));
                obj.setCep(rs.getString("cep"));
                obj.setEndereco(rs.getString("endereco"));
                obj.setNumero(rs.getInt("numero"));
                obj.setComplemento(rs.getString("complemento"));
                obj.setBairro(rs.getString("bairro"));
                obj.setCidade(rs.getString("cidade"));
                obj.setEstado(rs.getString("estado"));
            }

            rs.close();
            stmt.close();
            return obj;

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao buscar funcionário: " + erro);
            return null;
        }
    }

    public List<Funcionarios> Listar() {
        List<Funcionarios> lista = new ArrayList<>();

        try {
            String sql = "SELECT * FROM tb_funcionarios";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Funcionarios obj = new Funcionarios();
                obj.setId(rs.getInt("id"));
                obj.setNome(rs.getString("nome"));
                obj.setRg(rs.getString("rg"));
                obj.setCpf(rs.getString("cpf"));
                obj.setEmail(rs.getString("email"));
                obj.setSenha(rs.getString("senha"));
                obj.setCargo(rs.getString("cargo"));
                obj.setNivel_acesso(rs.getString("nivel_acesso"));
                obj.setTelefone(rs.getString("telefone"));
                obj.setCelular(rs.getString("celular"));
                obj.setCep(rs.getString("cep"));
                obj.setEndereco(rs.getString("endereco"));
                obj.setNumero(rs.getInt("numero"));
                obj.setComplemento(rs.getString("complemento"));
                obj.setBairro(rs.getString("bairro"));
                obj.setCidade(rs.getString("cidade"));
                obj.setEstado(rs.getString("estado"));

                lista.add(obj);
            }

            rs.close();
            stmt.close();

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao criar a lista: " + erro);
        }
        return lista;
    }

    public List<Funcionarios> Filtrar(String nome) {
        List<Funcionarios> lista = new ArrayList<>();

        try {
            String sql = "SELECT * FROM tb_funcionarios WHERE nome like ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Funcionarios obj = new Funcionarios();
                obj.setId(rs.getInt("id"));
                obj.setNome(rs.getString("nome"));
                obj.setRg(rs.getString("rg"));
                obj.setCpf(rs.getString("cpf"));
                obj.setEmail(rs.getString("email"));
                obj.setSenha(rs.getString("senha"));
                obj.setCargo(rs.getString("cargo"));
                obj.setNivel_acesso(rs.getString("nivel_acesso"));
                obj.setTelefone(rs.getString("telefone"));
                obj.setCelular(rs.getString("celular"));
                obj.setCep(rs.getString("cep"));
                obj.setEndereco(rs.getString("endereco"));
                obj.setNumero(rs.getInt("numero"));
                obj.setComplemento(rs.getString("complemento"));
                obj.setBairro(rs.getString("bairro"));
                obj.setCidade(rs.getString("cidade"));
                obj.setEstado(rs.getString("estado"));

                lista.add(obj);
            }

            rs.close();
            stmt.close();

        } catch (SQLException erro) {
            JOptionPane.showMessageDialog(null, "Erro ao criar a lista: " + erro);
        }
        return lista;
    }

    public void efetuarLogin(String email, String senha) {

        try {
            String sql = "SELECT * FROM tb_funcionarios WHERE email = ? AND senha = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, senha);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                if (rs.getString("nivel_acesso").equals("Administrador")) {

                    AreaTrabalho at = new AreaTrabalho();
                    at.usuarioLogado = rs.getString("nome");
                    JOptionPane.showMessageDialog(null, "Seja bem-vindo ao sistema!\n" + at.usuarioLogado);
                    at.setVisible(true);

                } else if (rs.getString("nivel_acesso").equals("Usuário")) {

                    AreaTrabalho at = new AreaTrabalho();

                    at.usuarioLogado = rs.getString("nome");
                    at.menuFornecedores.setVisible(false);
                    at.menuFuncionario.setEnabled(false);
                    at.menuEstoque.setEnabled(false);

                    JOptionPane.showMessageDialog(null, "Seja bem-vindo ao sistema!\n" + at.usuarioLogado);
                    at.setVisible(true);

                }

            } else {

                FormularioLogin login = new FormularioLogin();
                JOptionPane.showMessageDialog(null, "Dados inválidos!");
                login.setVisible(true);
            }

        } catch (SQLException erro) {

            JOptionPane.showMessageDialog(null, "Erros: " + erro);
        }
    }
}
