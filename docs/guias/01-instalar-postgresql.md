# Instalar o PostgreSQL e rodar o KnowFlow com ele

O TCC define o PostgreSQL como banco do sistema (RNF14, seção 3.1). O código já está
configurado para ele: falta só instalar o banco na máquina.

> **H2 x PostgreSQL:** o H2 é um banco que roda dentro da própria aplicação e some quando
> ela para. Ele é usado **apenas nos testes automáticos** (JUnit) e para rodar rápido sem
> instalar nada. O sistema "de verdade", o que vai para a banca, usa o PostgreSQL.

## 1. Instalar (uma vez só)

1. Baixe o instalador do **PostgreSQL 16** para Windows em
   <https://www.postgresql.org/download/windows/> (botão *Download the installer*, da EDB).
2. Execute e aceite os padrões. Deixe marcados **PostgreSQL Server**, **pgAdmin 4** e
   **Command Line Tools**.
3. Quando pedir a **senha do superusuário (postgres)**, use `postgres`. É a que está no
   `application.properties`. Se escolher outra, ajuste o arquivo (passo 3).
4. Porta: deixe **5432**. Locale: padrão.
5. No final, desmarque o *Stack Builder* (não é necessário).

## 2. Criar o banco

1. Abra o **pgAdmin 4** e entre com a senha `postgres`.
2. No painel da esquerda: *Servers > PostgreSQL 16 > Databases* > botão direito >
   **Create > Database...**
3. Nome: `fluxoconhecimento` > **Save**.

Pronto. **Não precisa criar as tabelas:** na primeira execução, a aplicação cria as 15
tabelas sozinha (`spring.jpa.hibernate.ddl-auto=update`) e também o administrador inicial.

> Se a banca pedir o script SQL, ele está em [`docs/banco/schema-postgresql.sql`](../banco/schema-postgresql.sql).
> Para rodar pelo pgAdmin: botão direito no banco > **Query Tool** > abrir o arquivo > **F5**.
> Rode o script **ou** deixe a aplicação criar as tabelas: não faça os dois.

## 3. Conferir a configuração

Arquivo `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fluxoconhecimento
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Se você usou outra senha na instalação, troque `spring.datasource.password`.

## 4. Rodar

No terminal, dentro da pasta `backend`:

```bash
mvn spring-boot:run
```

Espere a linha `Started FluxoConhecimentoApplication` e abra <http://localhost:8080>.

- Login inicial: **admin@knowflow.com / Admin1234**
- Para já ter usuários, categorias e problemas de exemplo (útil na apresentação):

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--knowflow.demo=true"
```

  Os dados de demonstração só são criados se o banco ainda não tiver problemas. Usuários
  criados (senha **Knowflow2026** para todos):

  | E-mail | Perfil | Departamento |
  |---|---|---|
  | gestor.ti@knowflow.com | Gestor | Tecnologia da Informação |
  | carlos@knowflow.com | Colaborador | Tecnologia da Informação |
  | beatriz@knowflow.com | Colaborador | Tecnologia da Informação |
  | gestor.rh@knowflow.com | Gestor | Recursos Humanos |
  | pedro@knowflow.com | Colaborador | Financeiro |

## 5. Ver os dados no pgAdmin

*Databases > fluxoconhecimento > Schemas > public > Tables*. Botão direito em uma tabela >
**View/Edit Data > All Rows**. Repare que a coluna `senha_hash` da tabela `usuario` guarda
só o hash BCrypt (RNF01), nunca a senha.

## 6. Backup diário (RNF11)

O script [`docs/banco/backup-diario.bat`](../banco/backup-diario.bat) gera uma cópia por dia
com o `pg_dump` e mantém os últimos 30 dias. Para agendar, abra o **Prompt de Comando como
administrador** e rode (ajuste o caminho):

```bat
schtasks /create /tn "KnowFlow backup" /sc daily /st 23:00 /tr "C:\caminho\para\backup-diario.bat"
```

## Problemas comuns

| Mensagem | Causa | O que fazer |
|---|---|---|
| `Connection to localhost:5432 refused` | O PostgreSQL não está rodando | Abra *Serviços* do Windows e inicie `postgresql-x64-16` |
| `password authentication failed for user "postgres"` | Senha diferente da do `application.properties` | Ajuste `spring.datasource.password` |
| `database "fluxoconhecimento" does not exist` | O banco não foi criado | Faça o passo 2 |
| `Port 8080 was already in use` | Outra cópia do sistema aberta | Feche o outro terminal (Ctrl+C) |
