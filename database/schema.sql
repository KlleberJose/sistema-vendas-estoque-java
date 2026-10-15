-- Schema derivado das consultas SQL dos DAOs (tipos e tamanhos inferidos).
-- Revise antes de usar em producao.
CREATE DATABASE IF NOT EXISTS sistemaestoque DEFAULT CHARACTER SET utf8mb4;
USE sistemaestoque;

CREATE TABLE tb_clientes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  rg VARCHAR(30),
  cpf VARCHAR(20),
  email VARCHAR(200),
  telefone VARCHAR(30),
  celular VARCHAR(30),
  cep VARCHAR(20),
  endereco VARCHAR(255),
  numero INT,
  complemento VARCHAR(200),
  bairro VARCHAR(100),
  cidade VARCHAR(100),
  estado VARCHAR(2)
);

CREATE TABLE tb_fornecedores (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  cnpj VARCHAR(20),
  email VARCHAR(200),
  telefone VARCHAR(30),
  celular VARCHAR(30),
  cep VARCHAR(20),
  endereco VARCHAR(255),
  numero INT,
  complemento VARCHAR(200),
  bairro VARCHAR(100),
  cidade VARCHAR(100),
  estado VARCHAR(2)
);

CREATE TABLE tb_funcionarios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  rg VARCHAR(30),
  cpf VARCHAR(20),
  email VARCHAR(200),
  senha VARCHAR(255),
  cargo VARCHAR(100),
  nivel_acesso VARCHAR(50),
  telefone VARCHAR(30),
  celular VARCHAR(30),
  cep VARCHAR(20),
  endereco VARCHAR(255),
  numero INT,
  complemento VARCHAR(200),
  bairro VARCHAR(100),
  cidade VARCHAR(100),
  estado VARCHAR(2)
);

CREATE TABLE tb_produtos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  descricao VARCHAR(100) NOT NULL,
  preco DECIMAL(10,2),
  qtd_estoque INT,
  for_id INT,
  FOREIGN KEY (for_id) REFERENCES tb_fornecedores(id)
);

CREATE TABLE tb_vendas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cliente_id INT,
  data_venda DATE,
  total_venda DECIMAL(10,2),
  observacoes TEXT,
  FOREIGN KEY (cliente_id) REFERENCES tb_clientes(id)
);

CREATE TABLE tb_itensvendas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  venda_id INT,
  produto_id INT,
  qtd INT,
  subtotal DECIMAL(10,2),
  FOREIGN KEY (venda_id) REFERENCES tb_vendas(id),
  FOREIGN KEY (produto_id) REFERENCES tb_produtos(id)
);