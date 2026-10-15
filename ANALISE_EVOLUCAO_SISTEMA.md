# Análise de Evolução do Sistema — SistemaDeVendasComEstoque

**Documento técnico de referência e roadmap**  
**Data da análise:** 24/09/2026  
**Escopo:** revisão completa do código-fonte atual (desktop Java/Swing + MySQL) com visão de produção empresarial e preparação para evolução com Spring Boot.

---

## Sumário executivo

O projeto atual é um **sistema desktop de ponto de venda (PDV) com estoque simples**, desenvolvido em Java 21 + Swing (NetBeans) e MySQL. A organização segue o padrão clássico `model` / `dao` / `view` / `jdbc`, sem camada de serviço, sem testes, sem migrações versionadas e com regras de negócio espalhadas nas telas.

**Veredito:** o sistema **não está apto** para uso empresarial de larga escala no estado atual. Ele pode funcionar para aprendizado ou operação muito pequena e controlada, mas apresenta riscos críticos de **integridade de estoque**, **segurança**, **concorrência**, **auditoria** e **manutenibilidade**.

### O que o sistema é hoje

| Capacidade | Situação |
|---|---|
| Cadastro de clientes, fornecedores, funcionários, produtos | Existe (CRUD básico) |
| Venda com carrinho e pagamento (dinheiro/cartão/cheque) | Existe, com falhas graves |
| Baixa de estoque na venda | Existe, sem transação/lock |
| Entrada de estoque | Existe, frágil |
| Histórico de vendas por período | Existe (consulta simples) |
| Total do dia | Existe |
| Login com níveis Administrador / Usuário | Existe, extremamente frágil |
| Filiais, setores, transferências, inventário, lotes, série, patrimônio | **Não existe** |
| API REST, auditoria, testes, CI/CD | **Não existe** |

### Classificação de prontidão

| Dimensão | Nota (0–5) | Comentário |
|---|---|---|
| Arquitetura | 1 | Acoplamento View↔DAO, sem domínio rico |
| Segurança | 0 | Senha em texto puro, credenciais no código |
| Integridade de estoque | 1 | Race conditions, sem movimentação histórica |
| Escalabilidade | 0 | `SELECT *` sem paginação, desktop monousuário implícito |
| Auditoria | 0 | Quase inexistente |
| Testabilidade | 0 | Pasta `test` vazia / sem testes |
| Prontidão produção | 0 | Bloqueadores críticos |

### Distinção importante sobre o escopo solicitado

Parte do briefing menciona operações típicas de **gestão patrimonial / almoxarifado multi-setor** (transferência, doação, furto, manutenção, responsável, localização, filiais). O código atual é um **PDV de varejo**. Esta análise cobre:

1. **Problemas reais do PDV atual** (necessários para qualquer produção).
2. **Evolução para PDV empresarial** (recomendado se o negócio for vendas).
3. **Extensão para inventário multi-local / patrimonial** (evolução futura, se for o alvo do negócio).

Não se deve misturar os dois modelos sem decisão explícita de produto.

---

## 1. Arquitetura do sistema

### 1.1 Estrutura atual

```
src/br/com/sistema/
├── model/          # POJOs anêmicos (Clientes, Produtos, Vendas, ...)
├── dao/            # SQL + JOptionPane + (em um caso) abertura de telas
├── view/           # JFrames/JDialogs NetBeans + regras de negócio
├── jdbc/           # Conexão hardcoded + utilitário de relatório
└── utilitarios/    # LimpaTela
```

Dependências relevantes: JDK 21, MySQL Connector, Swing. **Não há** Spring, JPA, Maven/Gradle (apenas Ant/NetBeans), Flyway, Security, testes.

Classe principal configurada em `nbproject/project.properties`: `br.com.sistema.jdbc.TestarConexao` — inadequada para execução do sistema (deveria ser `FormularioLogin`).

### 1.2 Problemas de organização e responsabilidades

| Problema | Onde | Por que vira problema |
|---|---|---|
| Regras de negócio na View | `FormularioPagamentos`, `FormularioVendas`, `FormularioEstoque` | Impossível reutilizar, testar ou expor API sem duplicar lógica |
| DAO acoplado à UI | Todos os DAOs usam `JOptionPane` | Camada de persistência não pode ser usada em batch/API/serviço |
| DAO abre telas | `FuncionariosDAO.efetuarLogin` instancia `AreaTrabalho` / `FormularioLogin` | Violação grave de SRP e DIP; autenticação misturada com navegação |
| Modelo anêmico | Todos os `model/*` | Sem invariantes (estoque negativo, preço inválido, etc.) |
| Herança inadequada | `Funcionarios extends Clientes`, `Fornecedores extends Clientes` | Funcionário não é um tipo de cliente; compartilha CPF/RG por acidente de modelagem |
| Conexão por instância de DAO | Construtor de cada DAO | Sem pool, sem fechamento consistente, risco de vazamento |
| Sem camada de aplicação | — | Orquestração de venda+itens+estoque fica na tela |

### 1.3 Acoplamentos desnecessários

- View → DAO concreto (sem interface).
- DAO → Swing.
- Autenticação → construção da área de trabalho.
- Estoque atualizado com valor absoluto calculado na UI (`quantidadeNova = atual - comprada`), em vez de operação atômica no banco (`qtd_estoque = qtd_estoque - ?`).

### 1.4 Classes que deveriam ser divididas ou reorganizadas

| Classe atual | Problema | Reorganização sugerida |
|---|---|---|
| `FuncionariosDAO` | CRUD + login + UI | `UsuarioRepository`, `AutenticacaoService`, controller/presenter |
| `ProdutosDAO` | CRUD + estoque | `ProdutoRepository` + `EstoqueService` |
| `FormularioPagamentos` | Pagamento + venda + estoque + itens | `VendaService.finalizarVenda(...)` |
| `FormularioEstoque` | Mistura cadastro e ajuste; campos trocados | Separar “cadastro de produto” de “movimento de estoque” |
| `ConexaoBanco` / `ConexaoBancoRelatorios` | Duplicação, driver legado no relatório | Um `DataSource` único + config externa |
| `Clientes` como pai de tudo | Modelo confuso | `Pessoa`/`Endereco` por composição, ou entidades distintas |

### 1.5 Clean Code / SOLID — diagnóstico

- **S:** quebrado (DAO+UI, View+negócio).
- **O:** difícil estender (novo tipo de movimento = alterar formulários).
- **L:** `Fornecedores`/`Funcionarios` como `Clientes` viola expectativa semântica.
- **I:** não há interfaces.
- **D:** tudo depende de concretos (`new ProdutosDAO()`).

### 1.6 Pacotes e camadas alvo (preparação Spring Boot)

```
com.empresa.sistema
├── domain/                 # entidades, value objects, enums, domain services
├── application/            # use cases / services de aplicação
├── infrastructure/
│   ├── persistence/        # JPA repositories, entities de persistência se necessário
│   ├── security/
│   └── config/
├── api/                    # REST controllers, DTOs, exception handlers
└── (opcional) desktop/ ou web/  # cliente
```

### 1.7 Padrões de projeto recomendados (somente onde resolvem dor real)

| Padrão | Uso | Motivo |
|---|---|---|
| Repository | Persistência | Desacoplar SQL/JPA da aplicação |
| Application Service / Use Case | `FinalizarVenda`, `RegistrarEntradaEstoque` | Centralizar transações e regras |
| Strategy / Enum tipado | Tipos de movimento de estoque | Evitar `if` espalhado |
| Unit of Work / `@Transactional` | Venda completa | Atomicidade |
| DTO + Mapper | API/UI | Não expor entidades cruas nem senhas |
| Outbox / Event (futuro) | Integrações | Só quando houver sistemas externos |

### 1.8 Adequação à evolução com Spring Boot

A arquitetura atual **não é adequada como está**, mas **é migrável** porque:

- O domínio funcional é pequeno e claro (PDV + estoque).
- Já há separação parcial model/dao/view.
- PreparedStatements predominam (bom ponto de partida).

**Estratégia de migração recomendada (incremental):**

1. Extrair regras das Views para Services Java puros (ainda sem Spring).
2. Trocar DAO por interfaces + implementação JDBC.
3. Introduzir Spring Boot (Web + Data JPA + Security + Validation).
4. Expor API REST; manter Swing temporariamente como cliente HTTP **ou** substituir por web.
5. Versionar schema com Flyway.
6. Adicionar testes e CI.

**Não recomendado:** “embrulhar” o Swing atual com Spring Boot sem separação de camadas — só aumenta complexidade sem ganho.

---

## 2. Spring Boot e tecnologias

Cada item abaixo só é sugerido se resolve um problema concreto deste sistema.

| Tecnologia | Problema que resolve neste sistema | Necessário agora? |
|---|---|---|
| **Spring Boot** | Base para DI, config, profiles, empacotamento e evolução | **Sim** (para evolução empresarial) |
| **Spring Web** | API REST para múltiplos clientes, filiais, integrações | **Sim**, se sair do monousuário desktop |
| **Spring Data JPA / Hibernate** | Reduz SQL repetido, relacionamentos, paginação | **Recomendado** |
| **Spring Transaction** | Corrige o maior risco: venda+itens+estoque sem atomicidade | **Crítico** |
| **Spring Security** | Autenticação/autorização real (hoje é cosmética) | **Crítico** |
| **Spring Validation** | Validações no backend (hoje quase só na UI, e frágeis) | **Crítico** |
| **Flyway ou Liquibase** | Schema versionado; hoje schema implícito no SQL dos DAOs | **Crítico** |
| **Spring Profiles + env vars** | Remover `root`/senha vazia hardcoded | **Crítico** |
| **Tratamento global de exceções** | Padronizar erros de API; evitar `JOptionPane` na infraestrutura | **Alta** |
| **Logs estruturados (JSON + correlation id)** | Diagnóstico em produção | **Alta** |
| **Spring Boot Actuator** | Health, métricas, readiness | **Alta** para produção |
| **Spring Cache** | Catálogo de produtos, categorias, fornecedores | **Crescimento** |
| **Spring Scheduling** | Jobs de inventário, fechamento diário, limpeza | **Crescimento** |
| **Spring Events** | Desacoplar “venda finalizada” → estoque/auditoria/notificação | **Crescimento** |
| **OpenAPI/Swagger** | Documentar API para integrações e frontend | **Alta** ao expor API |
| **Spring Boot Test** | Testes de serviço/API/segurança | **Crítico** antes de produção |
| **Secrets manager / vault (ou ao menos env)** | Credenciais e chaves | **Crítico** |
| **Message queue (Rabbit/SQS)** | Processamento assíncrono em volume muito alto | **Futuro** (não necessário no início) |
| **Redis** | Sessão/cache distribuído multi-instância | **Futuro** |

### Configuração e secrets (obrigatório)

Hoje:

```java
// ConexaoBanco.java / ConexaoBancoRelatorios.java
url = "jdbc:mysql://localhost/sistemaestoque";
usuario = "root";
senha = "";
```

Isso é **inaceitável** em produção. Usar `application-{profile}.yml` + variáveis de ambiente (`SPRING_DATASOURCE_*`), usuário MySQL com privilégios mínimos, e nunca commit de senhas.

---

## 3. Modelo de domínio

### 3.1 Entidades atuais (inferidas do código)

#### `Clientes` → `tb_clientes`
Campos: id, nome, rg, cpf, email, telefone, celular, cep, endereco, numero, complemento, bairro, cidade, estado.

**Problemas:**
- Sem validação de CPF, unicidade, pessoa física/jurídica.
- Endereço embutido (aceitável no início; normalizar depois se houver múltiplos endereços).
- Sem `ativo`, auditoria, data de cadastro.
- `numero` como `int` impede “S/N”.

#### `Funcionarios` → `tb_funcionarios` (extends Clientes)
Campos extras: senha, cargo, nivel_acesso.

**Problemas:**
- Herança de cliente é conceitualmente errada.
- Senha em texto puro.
- `nivel_acesso` como `String` livre (“Administrador”/“Usuário”) — frágil.
- Listagem retorna senha para a UI.
- Sem usuário de login separado de dados pessoais (email usado como login).

#### `Fornecedores` → `tb_fornecedores` (extends Clientes)
Campo extra: cnpj. Persistência não usa rg/cpf do pai.

**Problemas:** herança desnecessária; sem status; sem validação de CNPJ.

#### `Produtos` → `tb_produtos`
Campos: id, descricao, preco (`double`), qtdEstoque (`int`), fornecedores.

**Problemas graves para escala:**
- Sem SKU/código de barras/EAN.
- Sem categoria.
- Estoque como atributo único do produto (não há local/filial/lote).
- Um fornecedor por produto (N:1 fixo) — na prática produto pode ter vários fornecedores.
- `double` para dinheiro (erro de ponto flutuante).
- Sem custo, margem, unidade de medida, status (ativo/inativo).
- Sem distinção produto genérico vs item serializado/patrimonial.
- Exclusão física (`DELETE`) — perde histórico de vendas.

#### `Vendas` → `tb_vendas`
Campos: id, cliente, dataVenda (`String`), totalDaVenda (`double`), observacoes.

**Problemas:**
- Sem vendedor/usuário responsável.
- Sem status (aberta, paga, cancelada, estornada).
- Sem formas de pagamento persistidas.
- Sem filial/caixa.
- Data como `String` no modelo.
- Total pode divergir da soma dos itens (não há recálculo no backend).

#### `ItensVendas` → `tb_itensvendas`
Campos: id, venda, produto, quantidade, subtotal.

**Problemas:**
- Não guarda preço unitário histórico (usa preço atual do produto no detalhe).
- `listaItens` faz `setSubtotal(rs.getInt(...))` — possível truncamento.
- Sem desconto por item.

### 3.2 Modelo alvo mínimo para PDV empresarial

```
Usuario, Perfil, Permissao
Cliente, Fornecedor, Endereco (opcional)
CategoriaProduto
Produto (sku, descricao, precoVenda, custo, unidade, status, categoria)
EstoqueSaldo (produto_id, local_id, quantidade)   -- se multi-local
MovimentoEstoque (tipo, produto, qtd, custo, usuario, motivo, ref_documento, created_at)
Venda, ItemVenda (preco_unitario_snapshot, qtd, desconto, subtotal)
PagamentoVenda (forma, valor)
Filial / Caixa (quando multiunidade)
AuditoriaEvento
```

### 3.3 Se o alvo for inventário patrimonial / multi-setor

Além do acima, seriam necessárias entidades que **hoje não existem**:

- `Filial`, `Setor`, `Localizacao`
- `ItemEstoque` ou `Ativo` (individualizado, nº série, patrimônio)
- `Lote` / `Validade`
- `Responsavel`
- `TipoMovimento` (ENTRADA, TRANSFERENCIA, BAIXA, DESCARTE, DOACAO, PERDA, FURTO, MANUTENCAO, INVENTARIO, AJUSTE)
- `OrdemManutencao`
- `Inventario` + `InventarioItem` (contagem × sistema)

**Recomendação:** decidir o produto. Misturar PDV puro com patrimônio sem modelagem clara gera complexidade desnecessária.

### 3.4 Valores monetários, datas, IDs

| Tema | Atual | Recomendado |
|---|---|---|
| Dinheiro | `double` | `BigDecimal` + `DECIMAL(15,2)` no banco |
| Datas | `String` / formatação manual | `Instant`/`LocalDateTime` + coluna `DATETIME`/`TIMESTAMP` |
| IDs | `int` autoincrement | Manter `BIGINT` no crescimento; UUID só se multi-master |
| Soft delete | Não | `ativo`/`deleted_at` em cadastros e produtos com histórico |

---

## 4. Regras de negócio

### 4.1 Regras existentes (e fragilidade)

| Regra | Onde | Fragilidade |
|---|---|---|
| Só adiciona item se `estoque >= quantidade` | `FormularioVendas` | Só no momento do carrinho; **não revalida no pagamento** |
| Pagamento >= total | `FormularioPagamentos` | OK superficialmente; não persiste formas |
| Login por email+senha e esconde menus | `FuncionariosDAO` | UI-only; usuário pode abrir formulários por outros meios se souber; senha clara |
| Entrada soma quantidade | `FormularioEstoque` | Race condition; sem histórico |
| Histórico por intervalo de datas | `VendasDAO` | Sem paginação; sem filtro por usuário/filial |

### 4.2 Bugs de regra / implementação

1. **`dispose()` dentro do `for` de itens** em `FormularioPagamentos`: após o primeiro item, a janela é fechada e uma nova `FormularioVendas` é aberta — risco de venda parcial (itens/estoque inconsistentes); com vários itens, múltiplas janelas de PDV.
2. **`retornaUltimoIdVenda()` via `MAX(id)`** após insert: sob concorrência, pode associar itens à venda errada.
3. **Estoque por valor absoluto** (`SET qtd_estoque = ?`) em vez de incremento atômico.
4. **`FormularioEstoque` Salvar/Editar** mapeia `txtQuantidadeAtual` → `preco` e `txtQuantidadeNova` → `qtdEstoque`, e Salvar não define fornecedor — corrompe dados. Na pesquisa por descrição, “Qtd Atual” recebe o **preço**.
5. **Mesmo produto várias vezes no carrinho** pode ultrapassar estoque (soma não consolidada).
6. **Pagamentos não gravados** — impossível reconciliar caixa.
7. **Exclusão física** de produto/cliente com possíveis FKs — erro ou órfãos.
8. **Sem cancelamento/estorno** de venda.
9. **Preço do item no histórico** pode mudar se o preço do produto for editado depois.
10. **Mismatch de nível de acesso:** o combo em `FormularioFuncionarios` grava `"Usuario"` (sem acento), mas `efetuarLogin` compara com `"Usuário"`. O SELECT do login pode “passar” e **nenhuma área de trabalho é aberta** — usuário cadastrado pela própria UI fica inutilizável.
11. **Busca de produto não carrega `for_id`** — só o nome do fornecedor; edição posterior pode falhar ou gravar fornecedor inválido.
12. **Campo Desconto no PDV** existe na tela e **nunca é lido** (feature morta / expectativa falsa).
13. **Login com nível diferente** de `Administrador`/`Usuário` (ex.: typo): ResultSet encontra o usuário, mas nenhum `if` abre a tela.

### 4.3 Regras ausentes essenciais (PDV)

- Recálculo e validação de totais no backend.
- Reserva de estoque ou baixa atômica com checagem (`WHERE qtd_estoque >= ?`).
- Impedir estoque negativo.
- Não vender produto inativo.
- Unicidade de CPF/CNPJ/SKU.
- Permissões por operação (não só esconder menu).
- Cancelamento de venda com devolução de estoque auditada.
- Desconto com limite por perfil.
- Fechamento de caixa.
- Numeração de cupom/pedido idempotente.

### 4.4 Regras empresariais adicionais (inventário avançado — se aplicável)

- Transferência só entre locais existentes; origem deve ter saldo.
- Baixa/descarte/doação/perda/furto exigem motivo e perfil autorizado.
- Manutenção altera status sem necessariamente alterar quantidade.
- Inventário gera ajustes com documento próprio.
- Itens serializados: movimentação unitária, não por quantidade agregada.
- Lotes: FEFO/FIFO configurável.
- Rastreabilidade completa por documento.

### 4.5 Onde centralizar

Toda regra de estoque e venda deve viver em **serviços de domínio/aplicação**, nunca em `JFrame`. A UI apenas coleta dados e exibe resultado.

---

## 5. Estoque e grande volume

### 5.1 Situação atual

O estoque é um **inteiro na linha do produto**. Isso funciona só para:

- Um único depósito implícito.
- Produtos fungíveis sem lote/série.
- Poucos usuários simultâneos.

### 5.2 Problemas de consistência e concorrência

Cenário clássico (dois caixas vendem o último item):

1. Ambos leem `qtd_estoque = 1`.
2. Ambos passam na UI.
3. Ambos gravam `0` ou um grava `-1` dependendo da ordem.
4. Sem transação, venda pode gravar e estoque falhar (ou vice-versa).

**Solução mínima:**

```sql
UPDATE tb_produtos
SET qtd_estoque = qtd_estoque - ?
WHERE id = ? AND qtd_estoque >= ?;
-- rows affected == 0 → estoque insuficiente
```

Dentro de `@Transactional` junto com insert de venda/itens.

### 5.3 Performance com milhares/milhões

| Prática atual | Impacto em escala |
|---|---|
| `Listar()` sem `LIMIT` | Memory blow-up; UI trava |
| Filtro `LIKE %termo%` | Não usa índice bem |
| Uma conexão por DAO | Esgota conexões MySQL |
| Sem índices declarados no código/migração | Consultas lentas |
| Sem tabela de movimentos | Impossível auditar; recalcular saldo fica caro |
| Pesquisa por descrição exata | Frágil; precisa SKU/código |

### 5.4 Modelagem para volume

- **Saldo** separado de **movimento** (event sourcing leve de estoque).
- Índices em `(produto_id, local_id)`, `sku`, `data_venda`, `venda_id`.
- Paginação obrigatória.
- Para milhões de movimentos: particionamento por mês/ano (futuro).
- Produtos iguais comprados em momentos diferentes → **lotes** ou custo médio documentado; hoje impossível distinguir.

### 5.5 Lacunas vs cenários do briefing

| Cenário | Suportado? |
|---|---|
| Milhares de produtos | Parcialmente (UI/listagens quebram) |
| Centenas de milhares de movimentações | Não (nem há tabela de movimento) |
| Várias filiais/setores | Não |
| Produtos individualizados / série / patrimônio | Não |
| Em manutenção / indisponível / baixado | Não (só quantidade) |
| Operações simultâneas | Não seguro |

---

## 6. Banco de dados

### 6.1 Modelo lógico atual (inferido)

```
tb_clientes (id PK, nome, rg, cpf, email, telefone, celular, cep, endereco, numero, complemento, bairro, cidade, estado)
tb_funcionarios (id PK, ...dados pessoais..., senha, cargo, nivel_acesso)
tb_fornecedores (id PK, nome, cnpj, email, telefone, celular, cep, endereco, numero, complemento, bairro, cidade, estado)
tb_produtos (id PK, descricao, preco, qtd_estoque, for_id FK→fornecedores)
tb_vendas (id PK, cliente_id FK→clientes, data_venda, total_venda, observacoes)
tb_itensvendas (id PK?, venda_id FK, produto_id FK, qtd, subtotal)
```

**Não há script de criação versionado no repositório** — risco operacional alto.

### 6.2 Avaliação

| Aspecto | Avaliação |
|---|---|
| Normalização | Básica OK para PDV simples; endereço repetido; estoque desnormalizado no produto |
| PK/FK | Assumidas; exclusões sem política `ON DELETE` |
| Unicidade | CPF/CNPJ/email/SKU aparentemente sem UNIQUE no código |
| Tipos | `double`/`String` no Java; alinhar com `DECIMAL`/`DATETIME` |
| Índices | Necessários: `cpf`, `email`, `sku`, `data_venda`, `produto_id`, `venda_id`, `descricao` (fulltext futuro) |
| Paginação | Ausente |
| Histórico | Só vendas; sem auditoria de cadastros/estoque |
| Particionamento | Preparar `tb_movimento_estoque` e `tb_vendas` por data no futuro |

### 6.3 Consultas que ficarão lentas

- `SELECT * FROM tb_produtos JOIN fornecedores` (lista completa).
- `SELECT * FROM tb_funcionarios` (ainda traz senha).
- Histórico `BETWEEN` sem índice em `data_venda`.
- `MAX(id)` sob carga (além de incorreto semanticamente).
- Detalhe de venda que relê preço atual do produto.

### 6.4 Estratégias para crescimento

1. Flyway com schema inicial + constraints.
2. `BIGINT` para IDs de movimento/venda.
3. Tabela de movimentos append-only.
4. Arquivamento/partição anual de movimentos e vendas antigas.
5. Read replicas só quando métricas justificarem (futuro).

---

## 7. Segurança

### 7.1 Achados críticos

| Risco | Evidência | Impacto |
|---|---|---|
| Senha em texto puro | `INSERT/SELECT senha`; login compara string direta | Compromete todos os usuários se o banco vazar |
| Credenciais DB no código | `ConexaoBanco` root sem senha | Acesso total ao banco |
| Autorização só escondendo menu | `menuFornecedores.setVisible(false)` etc. | Falsa segurança |
| Senhas listadas no grid | `FuncionariosDAO.Listar` | Exposição interna |
| `txtSenha.getText()` | `FormularioLogin` | Má prática com `JPasswordField` |
| Sem sessão/expiração | Desktop abre e fica | Terminal abandonado |
| Sem rate limit de login | — | Força bruta trivial |
| SQL dinâmico em relatórios | `ConexaoBancoRelatorios.executeSQL(String sql)` | Vetor de SQL injection se alimentado por UI |
| Driver JDBC legado na classe de relatório | `com.mysql.jdbc.Driver` | Obsoleto |
| Sem HTTPS/API | N/A desktop local | Ao virar API, obrigatório TLS |

PreparedStatements nos DAOs principais **mitigam SQL injection** nas operações CRUD — ponto positivo.

### 7.2 Controles obrigatórios antes de produção

1. Hash de senha (BCrypt/Argon2) via Spring Security.
2. RBAC real (perfis → permissões → endpoints/use cases).
3. Remover senha de qualquer DTO de listagem.
4. Secrets fora do código.
5. Usuário MySQL dedicado com grants mínimos.
6. Auditoria de login (sucesso/falha, IP se aplicável).
7. Timeout de sessão / bloqueio de tela.
8. Validação server-side de todas as entradas.
9. Ao expor API: JWT ou sessão + CSRF conforme modelo; CORS restrito; rate limiting.
10. Proteção IDOR: checar se o usuário pode acessar o recurso/filial.
11. Mascarar CPF/dados sensíveis em logs.

### 7.3 Mass assignment / IDOR

Hoje a UI monta o objeto completo; ao migrar para API, **nunca** aceitar `nivel_acesso`, `qtd_estoque` ou `preco` livres sem autorização em endpoints genéricos de update.

---

## 8. Auditoria e rastreabilidade

### 8.1 Situação atual

- Venda tem data, cliente, total, observações e itens.
- **Não há** quem vendeu.
- **Não há** histórico de alteração de preço, estoque, cadastros.
- Entrada de estoque não gera documento.
- Pagamento não é registrado como entidade.
- Impossível reconstruir “por que o estoque mudou”.

### 8.2 Modelo de auditoria recomendado

**A) Auditoria de movimentos de estoque (obrigatória)**  
Cada alteração de quantidade gera linha imutável:

- produto_id, local_id (se houver)
- tipo_movimento
- quantidade (+/-)
- saldo_anterior, saldo_posterior (opcional, mas útil)
- usuario_id, data/hora
- documento_origem (venda_id, ajuste_id, …)
- motivo
- ip/origem (quando fizer sentido)

**B) Auditoria de entidades (cadastros)**  
Tabela genérica ou Envers/Hibernate Envers:

- entidade, entidade_id, campo, valor_anterior, valor_novo, usuario, timestamp

**C) Trilha de segurança**  
Login, logout, falhas, mudanças de permissão.

### 8.3 Resultado esperado

Consulta: “histórico do produto X” → lista cronológica de entradas, vendas, ajustes, com usuário e motivo.

---

## 9. Performance e escalabilidade

### 9.1 Problemas relevantes **agora**

- Ausência de transação na venda (correção > otimização).
- Race condition de estoque.
- `dispose` no loop (bug funcional).
- Carregar listas inteiras na ativação da janela (`formWindowActivated` → `listar()`).
- Conexões sem pool/fechamento.

### 9.2 Tratar preventivamente para crescimento

- Paginação (UI e API).
- Índices.
- Cache de catálogo.
- Evitar N+1 (JPA: fetch join / entity graphs conscientes).
- Jobs assíncronos para relatórios pesados e importações.
- Timeouts e circuit breakers quando houver integrações.

### 9.3 O que **não** fazer agora

- Microserviços.
- Kafka.
- Sharding.
- CQRS completo.

Complexidade prematura. Um modular monolith Spring Boot bem transacional resolve o horizonte próximo.

---

## 10. API e integração

### 10.1 Necessidade

Para larga escala (vários caixas, filiais, app, e-commerce, ERPs), **sim**: o núcleo deve ser API REST (ou REST+eventos). O Swing atual pode ser cliente temporário.

### 10.2 Esboço de recursos

```
POST   /api/v1/auth/login
GET    /api/v1/produtos?page&size&sku&descricao
POST   /api/v1/produtos
POST   /api/v1/estoque/entradas
POST   /api/v1/vendas                    # idempotente com Idempotency-Key
GET    /api/v1/vendas/{id}
POST   /api/v1/vendas/{id}/cancelamentos
GET    /api/v1/relatorios/vendas-dia
```

### 10.3 Padrões

- DTOs de entrada/saída; nunca expor senha.
- Status HTTP corretos (400 validação, 401/403 authz, 409 conflito de estoque, 404).
- Erros no formato Problem Details (RFC 7807).
- Paginação `page/size/sort`.
- Versionamento `/v1`.
- OpenAPI.
- Idempotência em `POST /vendas` (evita duplicar venda em retry de rede).
- Importação CSV/Excel de produtos (assíncrona).

---

## 11. Interface e experiência de uso

### 11.1 Telas atuais

Login, Área de trabalho (menus), Clientes, Funcionários, Fornecedores, Produtos, Estoque, Vendas, Pagamentos, Histórico, Detalhe da venda, Total do dia.

### 11.2 Problemas de UX / erro humano

| Problema | Impacto |
|---|---|
| Estoque e cadastro misturados / campos trocados | Corrupção de preço e quantidade |
| Pouca validação de campos vazios | `NumberFormatException` e crashes |
| Texto livre onde deveria ser seleção (UF, cargo, nível) | Dados inconsistentes; combo nível com `"Usuario"` ≠ login `"Usuário"` |
| Carrinho permite ultrapassar estoque com itens repetidos | Over-selling |
| Total do carrinho não sincroniza se o usuário editar células | Divergência visual × cobrado |
| Sem remoção de item do carrinho | Operador precisa cancelar a venda inteira |
| Campo Desconto visível e inerte | Confusão operacional |
| Botões Cancelar (login/PDV) sem ActionListener | Expectativa quebrada |
| Mensagem “ID da última venda” ao usuário final | Ruído / exposição interna |
| Botões Imprimir em CRUDs/detalhe sem implementação | Feature morta |
| Cabeçalhos vs. ordem das colunas desalinhados em grids de CRUD | Clique preenche campos errados na percepção do usuário |
| Combo de fornecedor em produtos só carrega no clique | Cadastro incompleto via teclado/Enter |
| Adicionar estoque sem selecionar linha (`idProduto = 0`) | Atualiza registro errado ou falha |
| `jLabel8` residual em pagamentos; label de estoque diz “Cadastro de Produtos” | Acabamento incompleto |
| Placeholder `lblLogado` com nome fixo antes do `windowActivated` | Vazamento de dado de desenvolvimento |
| Login falho abre nova janela sem fechar a anterior | Acúmulo de janelas |
| Usuário comum ainda acessa produtos/vendas sem granularidade | Risco operacional |
| Sem confirmação forte em exclusões críticas | Perda de dados |
| Sem indicação de estoque mínimo | Não previne ruptura |
| Máscara de data `##/##/20##` | Anos fora de 2000–2099 inviáveis |

### 11.3 Melhorias sugeridas

- Fluxo de venda: busca por código de barras → quantidade → enter (PDV real).
- Consolidar linhas iguais no carrinho.
- Campos obrigatórios marcados; máscaras de CPF/CNPJ/CEP.
- Separar tela “Ajuste de estoque” com motivo.
- Perfis: operador de caixa vs estoquista vs administrador.
- Feedback claro de erro de negócio (“estoque insuficiente”) vs erro técnico.

---

## 12. Relatórios e indicadores

### 12.1 Existentes

- Histórico de vendas por período.
- Total vendido no dia.
- Impressão de produtos não implementada.

### 12.2 Relatórios necessários (PDV empresarial)

| Relatório | Motivo |
|---|---|
| Vendas por período / caixa / vendedor | Gestão e conferência |
| Itens mais vendidos | Compras |
| Margem (preço − custo) | Financeiro |
| Posição de estoque | Operação |
| Abaixo do mínimo / ruptura | Reposição |
| Entradas vs saídas | Conferência |
| Cancelamentos/estornos | Fraude e qualidade |
| Curva ABC | Planejamento |
| Por fornecedor / categoria | Compras |

### 12.3 Se evoluir para multi-local / patrimônio

Inventário por setor/localização/status, transferências, manutenções, perdas, produtos sem responsável/local, tempo parado, etc. — todos dependem do novo modelo.

Relatórios pesados devem rodar **fora da thread da UI**, com exportação CSV/PDF.

---

## 13. Testes

### 13.1 Situação

Não há testes automatizados (`test.src.dir=test`, sem arquivos).

### 13.2 Obrigatórios antes de produção

| Área | Tipo | Exemplos |
|---|---|---|
| Finalizar venda | Unitário + integração | Estoque insuficiente; pagamento parcial; soma de itens |
| Baixa/entrada estoque | Integração + concorrência | Duas threads no último item |
| Autenticação | Segurança | Senha hash; usuário inválido; permissão negada |
| Validações | Unitário | CPF, preço negativo, quantidade zero |
| Cancelamento | Integração | Devolve estoque uma única vez |
| Repositórios | @DataJpaTest | Constraints UNIQUE |
| API | MockMvc/Testcontainers | Contratos HTTP |
| Regressão | CI | Suite mínima a cada commit |

### 13.3 Meta prática

Cobertura alta nos **serviços de venda e estoque** (não perseguir 100% de Views Swing). Com API, priorizar testes de API + domínio.

---

## 14. Observabilidade e produção

Necessário para operar:

- Logs estruturados (operação, usuarioId, vendaId).
- Actuator: `/health`, `/metrics`, `/info`.
- Alertas: erro de pagamento, falha de DB, taxa de 409 de estoque, latência p95.
- Rastreamento de erros (Sentry/OpenTelemetry — crescimento).
- Backup MySQL automatizado + teste de restore.
- Ambientes: local / homologação / produção.
- Runbook de incidente e rollback de migração Flyway.

Hoje: apenas `JOptionPane` e `System`-less logging pontual — insuficiente.

---

## 15. DevOps e implantação

### 15.1 Estado atual

Projeto Ant/NetBeans, JAR desktop, dependência MySQL local, sem CI/CD, sem Docker, main class de teste de conexão.

### 15.2 Estrutura recomendada

```
dev → homolog → prod
Git + PR + CI (build, testes, análise estática)
Flyway no startup (ou job separado)
Secrets no ambiente / vault
Docker Compose (app + mysql) para dev/homolog
Backup diário + retenção
Deploy versionado + rollback (imagem anterior)
```

### 15.3 Docker

Faz sentido **para o backend Spring Boot + MySQL** em homolog/prod. Para o cliente Swing, instalador/MSI ou WebUI é decisão à parte.

### 15.4 Build

Migrar de Ant para **Maven ou Gradle** facilita Spring Boot, testes e CI.

---

## 16. Problemas atuais e dívida técnica

### 16.1 Críticos (P1)

1. Senhas em texto puro.
2. Credenciais de banco no código.
3. Venda sem transação atômica.
4. Concorrência de estoque (read-modify-write).
5. `MAX(id)` para amarrar itens à venda.
6. `dispose()` dentro do loop de itens no pagamento.
7. Bug de mapeamento de campos em `FormularioEstoque`.
8. Autorização apenas visual.
9. Sem schema versionado.
10. Sem testes das regras centrais.
11. Mismatch `"Usuario"` (cadastro) vs `"Usuário"` (login) — usuário criado pela UI pode não abrir o sistema.
12. Busca de produto sem carregar `for_id`.

### 16.2 Importantes (P2)

- DAO acoplado a Swing.
- Login dentro do DAO abrindo telas.
- `double` para dinheiro.
- Listagens sem paginação.
- Sem registro de pagamentos / vendedor.
- Herança Funcionario/Fornecedor → Cliente.
- Exclusão física sem política.
- Sem histórico de movimento de estoque.
- Main class incorreta.
- Driver legado em relatórios.

### 16.3 Médio prazo (P3)

- Categorias, SKU, estoque mínimo.
- RBAC fino.
- Relatórios gerenciais.
- API REST + OpenAPI.
- Soft delete e auditoria de cadastros.
- Cache e jobs.

### 16.4 Desejáveis / futuro (P4)

- Multi-filial/setor/transferências.
- Lotes, séries, patrimônio.
- Filas, particionamento, read replicas.
- Substituição completa do Swing por web.
- Integrações fiscais/TEF.

### 16.5 Código a refatorar / remover / substituir

| Ação | Alvo |
|---|---|
| Refatorar | Extrair `VendaService`, `EstoqueService`, `AutenticacaoService` |
| Remover da camada DAO | `JOptionPane`, imports de `view` |
| Substituir | `ConexaoBanco*` por DataSource/pool |
| Substituir | Comparação de senha plain text |
| Corrigir ou remover | Botões/handlers vazios (imprimir) |
| Substituir | Ant → Maven/Gradle na migração Spring |

---

## 17. Plano de evolução (priorizado)

Legenda de flags: **BD** banco · **RN** regra de negócio · **API** · **UI**

### Prioridade 1 — Crítico (antes de qualquer produção real)

| # | Problema | Solução | Motivo | Impacto | Deps | Spring | BD | RN | API | UI |
|---|---|---|---|---|---|---|---|---|---|---|
| 1.1 | Senha em texto puro | Hash BCrypt; migrar usuários | Vazamento = comprometimento total | Altíssimo | — | Security | Sim | Sim | Sim | Sim |
| 1.2 | Credenciais hardcoded | Env/profiles; user DB dedicado | Acesso indevido ao MySQL | Altíssimo | — | Profiles | Não* | Não | Não | Não |
| 1.3 | Venda não atômica | Transação única venda+itens+estoque | Dados inconsistentes | Altíssimo | 1.5 | `@Transactional` | Não | Sim | Sim | Sim |
| 1.4 | Race de estoque | `UPDATE ... WHERE qtd >= ?` | Over-selling | Altíssimo | 1.3 | Transaction | Sim | Sim | Sim | Sim |
| 1.5 | `MAX(id)` | `getGeneratedKeys` / retorno JPA | Itens na venda errada | Altíssimo | — | Data JPA | Não | Sim | Sim | Não |
| 1.6 | `dispose()` no loop | Fechar somente após processar todos os itens / mover para service | Venda parcial | Alto | 1.3 | — | Não | Sim | Não | Sim |
| 1.7 | Bug FormularioEstoque | Corrigir binding; separar ajuste de cadastro | Corrompe preço/estoque | Alto | — | — | Não | Sim | Não | Sim |
| 1.8 | Sem revalidação no pagamento | Validar estoque novamente ao finalizar | Carrinho obsoleto | Alto | 1.3–1.4 | Validation | Não | Sim | Sim | Sim |
| 1.9 | Schema não versionado | Flyway com script baseline | Impossível promover ambientes | Alto | — | Flyway | Sim | Não | Não | Não |
| 1.10 | Zero testes críticos | Testes de venda/estoque/auth | Regressões silenciosas | Alto | 1.3 | Boot Test | Não | Não | Sim | Não |
| 1.11 | `"Usuario"` vs `"Usuário"` no login | Enum/constante única; migrar dados; testes | Cadastro pela UI gera usuário que não entra | Alto | 1.1 | — | Sim* | Sim | Não | Sim |
| 1.12 | Busca de produto sem `for_id` | Preencher FK completa no ResultSet | Edição quebra fornecedor | Médio-Alto | — | — | Não | Não | Não | Sim |

\*migração/normalização do valor já gravado em `nivel_acesso`.

\*exceto criação do usuário/grants no MySQL.

### Prioridade 2 — Alta

| # | Problema | Solução | Motivo | Impacto | Deps | Spring | BD | RN | API | UI |
|---|---|---|---|---|---|---|---|---|---|---|
| 2.1 | Autorização cosmética | RBAC por permissão no backend | Fraude/uso indevido | Alto | 1.1 | Security | Sim | Sim | Sim | Sim |
| 2.2 | Regras na View / DAO com Swing | Camada application + interfaces | Manutenção e testes | Alto | — | DI | Não | Não | Sim | Sim |
| 2.3 | Sem movimentação de estoque | Tabela `movimento_estoque` | Rastreabilidade | Alto | 1.3 | JPA | Sim | Sim | Sim | Sim |
| 2.4 | Pagamentos não persistidos | `pagamento_venda` | Conciliação de caixa | Alto | 1.3 | JPA | Sim | Sim | Sim | Sim |
| 2.5 | Sem vendedor na venda | `usuario_id` obrigatório | Responsabilização | Alto | 2.1 | — | Sim | Sim | Sim | Sim |
| 2.6 | `double` monetário | `BigDecimal` + `DECIMAL` | Erros financeiros | Alto | — | — | Sim | Sim | Sim | Sim |
| 2.7 | Listagens completas | Paginação + busca por SKU | Performance UI/DB | Alto | — | Data | Não | Não | Sim | Sim |
| 2.8 | Herança indevida | Entidades separadas / composição | Modelo claro | Médio | — | JPA | Sim | Sim | Sim | Sim |
| 2.9 | Exclusão física | Soft delete + bloqueio se houver vínculos | Integridade histórica | Alto | 2.3 | — | Sim | Sim | Sim | Sim |
| 2.10 | Sem auditoria de segurança | Log de login e ações críticas | Compliance | Alto | 2.1 | — | Sim | Não | Sim | Parcial |
| 2.11 | Projeto Ant / main errada | Maven/Gradle + entrypoint Login/API | Build reproduzível | Médio | — | Boot | Não | Não | Não | Não |
| 2.12 | Preço histórico do item | Snapshot em `item_venda` | Relatórios corretos | Alto | 1.3 | — | Sim | Sim | Sim | Não |

### Prioridade 3 — Média

| # | Problema | Solução | Motivo | Impacto | Deps | Spring | BD | RN | API | UI |
|---|---|---|---|---|---|---|---|---|---|---|
| 3.1 | Sem SKU/categoria/estoque mín. | Evoluir cadastro produto | Operação real de loja | Médio | 2.2 | Validation | Sim | Sim | Sim | Sim |
| 3.2 | Sem API | Spring Web + OpenAPI | Multi-caixa/integrações | Médio | 2.2 | Web/OpenAPI | Não | Não | Sim | Opcional |
| 3.3 | Relatórios pobres | Endpoints/relatórios + export | Gestão | Médio | 2.3–2.4 | — | Não | Não | Sim | Sim |
| 3.4 | Cancelamento/estorno | Fluxo com devolução auditada | Operação e fraude | Médio | 2.3 | Transaction | Sim | Sim | Sim | Sim |
| 3.5 | UX PDV | Atalhos, código barras, consolidação carrinho | Produtividade | Médio | 1.x | — | Não | Sim | Não | Sim |
| 3.6 | Observabilidade | Actuator + logs JSON + alertas | Operar produção | Médio | 3.2 | Actuator | Não | Não | Sim | Não |
| 3.7 | Cache catálogo | Spring Cache | Performance leitura | Médio | 3.2 | Cache | Não | Não | Sim | Não |
| 3.8 | Importação produtos | Job assíncrono | Onboarding | Médio | 3.2 | Scheduling/Events | Não | Sim | Sim | Sim |
| 3.9 | Ambientes CI/CD | Pipeline + Docker homolog | Qualidade de entrega | Médio | 2.11 | Profiles | Sim | Não | Não | Não |

### Prioridade 4 — Baixa / evolução futura

| # | Problema | Solução | Motivo | Impacto | Deps | Spring | BD | RN | API | UI |
|---|---|---|---|---|---|---|---|---|---|---|
| 4.1 | Sem filiais/setores | Modelo multi-local + saldo por local | Expansão geográfica | Alto futuro | 2.3 | — | Sim | Sim | Sim | Sim |
| 4.2 | Sem transferências | Documento de transferência | Almoxarifado | Alto futuro | 4.1 | — | Sim | Sim | Sim | Sim |
| 4.3 | Sem lotes/série/patrimônio | Itens individualizados | Rastreio fino | Alto futuro | 4.1 | — | Sim | Sim | Sim | Sim |
| 4.4 | Doação/furto/manutenção | Tipos de movimento + status | Inventário avançado | Médio futuro | 2.3 | Events | Sim | Sim | Sim | Sim |
| 4.5 | Inventário cíclico | Módulo contagem × sistema | Acurácia | Médio futuro | 2.3 | Scheduling | Sim | Sim | Sim | Sim |
| 4.6 | Volume extremo | Particionamento, filas, cache distribuído | Escala | Alto futuro | 3.x | Cache/Messaging | Sim | Não | Sim | Não |
| 4.7 | Substituir Swing | Web (React/Thymeleaf) ou outro cliente | Manutenção UI | Médio futuro | 3.2 | Web | Não | Não | Sim | Sim |

---

## 18. Roadmap sugerido (fases)

### Fase A — Estabilização do PDV atual (2–4 semanas)
Corrigir P1 sem necessariamente Spring: serviços Java, transações JDBC (`conn.setAutoCommit(false)`), hash de senha, Flyway baseline, corrigir bugs de estoque/pagamento, testes de integração com Testcontainers/MySQL.

### Fase B — Fundação Spring Boot (4–8 semanas)
Migrar para Spring Boot modular monolith: JPA, Security, Validation, Actuator, Profiles, OpenAPI. Manter Swing como cliente ou começar UI web mínima.

### Fase C — Operação empresarial PDV (4–6 semanas)
Movimentos de estoque, pagamentos, RBAC, relatórios, paginação, cancelamento, CI/CD, backups, homologação.

### Fase D — Escala e multiunidade (sob demanda)
Filiais, locais, transferências, lotes/séries, inventário, patrimônio — **somente após decisão de produto**.

---

## Classificação: necessário × recomendado × futuro

### Necessário para produção (mesmo em empresa pequena)

- Transações + estoque atômico  
- Hash de senha + secrets externos  
- Correção dos bugs de pagamento/estoque  
- Flyway  
- Auditoria mínima de movimentos e vendas (com usuário)  
- Testes do núcleo  
- Soft delete / não apagar histórico  
- `BigDecimal` para dinheiro  

### Recomendado para crescimento

- Spring Boot + API  
- RBAC fino  
- Paginação, índices, cache  
- Relatórios gerenciais  
- Observabilidade e CI/CD  
- SKU, categorias, estoque mínimo  

### Evolução futura (não essencial no dia 1)

- Microserviços, filas pesadas, particionamento  
- Multi-filial/patrimonial completo  
- Troca total de UI  
- Integrações fiscais avançadas  

---

## Conclusão

O sistema demonstra um **PDV educacional funcional em caminhos felizes**, mas a implementação atual concentra risco demais na interface, não protege a integridade do estoque sob concorrência, não audita o que importa e não está estruturado para crescer.

O caminho mais seguro não é “adicionar Spring Boot por cima”, e sim:

1. **Corrigir o núcleo transacional e de segurança**,  
2. **Extrair regras para uma camada de aplicação testável**,  
3. **Versionar o banco**,  
4. **Só então** adotar Spring Boot como plataforma de evolução (API, Security, Data, Actuator),  
5. **Expandir o domínio** (filiais, patrimônio, etc.) somente com decisão clara de negócio.

Este documento deve ser tratado como **baseline vivo**: cada item do plano (seção 17) pode virar issue/épico no controle de versão, com aceite mensurável (teste automatizado + critério de produção).

---

## Apêndice A — Mapa rápido de arquivos críticos

| Arquivo | Papel | Observação principal |
|---|---|---|
| `jdbc/ConexaoBanco.java` | Conexão | Credenciais hardcoded |
| `jdbc/ConexaoBancoRelatorios.java` | SQL genérico | Driver legado + `executeSQL` |
| `dao/FuncionariosDAO.java` | CRUD + login | Senha plain; abre Views |
| `dao/ProdutosDAO.java` | CRUD + estoque | Baixa por valor absoluto |
| `dao/VendasDAO.java` | Vendas | `MAX(id)` |
| `dao/ItensVendasDAO.java` | Itens | Sem transação compartilhada |
| `view/FormularioPagamentos.java` | Finalização | Bug do `dispose` no loop; orquestra tudo |
| `view/FormularioVendas.java` | PDV | Validação de estoque só no carrinho |
| `view/FormularioEstoque.java` | Estoque | Binding incorreto Salvar/Editar |
| `model/*` | Domínio | Anêmico; heranças questionáveis |

## Apêndice B — Tabelas mínimas a criar na evolução (além das atuais)

```
tb_usuarios (separado de dados de pessoa, se desejado)
tb_perfis / tb_permissoes / tb_perfil_permissao
tb_categorias
tb_movimento_estoque
tb_pagamento_venda
tb_auditoria (ou equivalente)
-- futuro:
tb_filiais, tb_locais, tb_estoque_saldo
tb_lotes, tb_itens_serializados
tb_inventarios, tb_inventario_itens
```

---

*Fim do documento de análise.*
