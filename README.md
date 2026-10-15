# Sistema de Vendas com Controle de Estoque (PDV) — Java

Sistema desktop de **ponto de venda (PDV) com controle de estoque**, desenvolvido em **Java 21 + Swing** com persistência em **MySQL via JDBC** e organização em camadas no padrão **DAO**.

## Funcionalidades

- Login de funcionários com níveis de acesso (Administrador / Usuário)
- Cadastro completo (CRUD, busca e filtro) de **clientes**, **fornecedores**, **funcionários** e **produtos**
- Busca de cliente por nome ou CPF e de produto por descrição ou código
- **Venda** com carrinho de itens e pagamento (dinheiro, cartão, cheque)
- **Baixa automática de estoque** na venda e **entrada de estoque**
- **Histórico de vendas** por período e detalhe dos itens de cada venda
- **Total vendido no dia**

## Stack

- Java 21
- Swing (NetBeans GUI Builder)
- JDBC com `PreparedStatement`
- MySQL 8 (MySQL Connector/J 9.6.0, em `lib/`)
- Build: Apache Ant (projeto NetBeans)

## Arquitetura

```
src/br/com/sistema/
├── model/        # entidades: Clientes, Fornecedores, Funcionarios, Produtos, Vendas, ItensVendas
├── dao/          # acesso a dados (SQL via JDBC)
├── view/         # telas Swing
├── jdbc/         # conexão com o banco (configuração externa em db.properties)
└── utilitarios/
```

## Como executar

1. Crie o banco executando `database/schema.sql` no MySQL
   (schema derivado das consultas dos DAOs — revise tipos/tamanhos).
2. Copie `db.properties.example` para `db.properties` na raiz do projeto e ajuste URL, usuário e senha
   (ou defina as variáveis de ambiente `DB_URL`, `DB_USER`, `DB_PASSWORD`).
3. Abra o projeto no **NetBeans** e execute (classe principal: `br.com.sistema.view.FormularioLogin`).
4. Cadastre um funcionário diretamente na tabela `tb_funcionarios` para o primeiro acesso.

## Roadmap — evolução para Spring Boot

A análise técnica completa está em [`ANALISE_EVOLUCAO_SISTEMA.md`](ANALISE_EVOLUCAO_SISTEMA.md). Próximos passos:

- [ ] Camada de serviço com regras de negócio fora das telas
- [ ] Transações na venda (venda + itens + baixa de estoque atômicas)
- [ ] Hash de senhas (BCrypt)
- [ ] Testes automatizados (JUnit 5 + Mockito)
- [ ] Migração para **Spring Boot** com API REST, Spring Data JPA, Spring Security, Flyway e Docker