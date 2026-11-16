# Subir o backend no GitHub (repositório knowflow-backend)

Segue a seção 7.1 do TCC: organização `knowflow-tcc`, segundo repositório.
Mesmo jeito do frontend: a pasta original **não sai do lugar**, sobe uma **cópia**.

Como vai ficar no seu computador:

```
C:\knowflow\
├── knowflow-frontend\   (já existe - foi o que você subiu)
├── frontend\            (clone do frontend com este nome - o backend lê ../frontend)
└── knowflow-backend\    (o que vamos subir agora)
    ├── pom.xml
    ├── src\
    ├── docs\
    ├── .gitignore
    └── README.md
```

## 1. No site do GitHub

Dentro da organização `knowflow-tcc`: **New repository**
- Owner: `knowflow-tcc`
- Repository name: `knowflow-backend`
- Description: `Backend do KnowFlow - API REST em Java 17 e Spring Boot`
- **Public**
- **NÃO** marque README, .gitignore nem licença (tem que nascer vazio)
- **Create repository**

## 2. No PowerShell: montar a pasta

```powershell
$origem = "C:\Users\Breno\OneDrive\Documentos\TCC\fluxo-conhecimento"
New-Item -ItemType Directory -Force C:\knowflow\knowflow-backend
cd C:\knowflow\knowflow-backend

# código do backend (sem a pasta target, que é gerada pelo build)
Copy-Item "$origem\backend\pom.xml" .
Copy-Item -Recurse "$origem\backend\src" .

# documentação (sem overleaf e pdf, que são do grupo, e sem o guia 03, desatualizado)
Copy-Item -Recurse "$origem\docs" .
Remove-Item -Recurse -Force docs\overleaf, docs\pdf
Remove-Item -Force docs\guias\03-github-por-etapas.md
```

### 2.1 Criar o `.gitignore`

```powershell
notepad .gitignore
```

O Bloco de Notas vai perguntar se quer criar o arquivo: **Sim**. Cole, salve e feche:

```
# Build do Maven
target/

# IDEs
.idea/
*.iml
.vscode/
.settings/
.project
.classpath

# Sistema operacional
.DS_Store
Thumbs.db
desktop.ini

# Logs e arquivos locais
*.log
backups/
```

Confira que o nome ficou certo (não pode ser `.gitignore.txt`):

```powershell
Get-ChildItem -Force
```

### 2.2 Criar o `README.md`

```powershell
notepad README.md
```

```markdown
# KnowFlow - Backend

API REST do sistema KnowFlow (TCC - UCSal): gestão do conhecimento sobre problemas,
causas e soluções, com busca por índice hash, categorias em árvore, histórico,
painel de indicadores e relatórios em PDF.

Java 17, Spring Boot 3, Spring Security (BCrypt), Spring Data JPA e PostgreSQL.
Camadas: Controller → Service → Repository → Model, com DTOs.

## Pré-requisitos
JDK 17, Maven e PostgreSQL com um banco chamado `fluxoconhecimento`
(guia em `docs/guias/01-instalar-postgresql.md`).

## Como rodar
1. Clone os dois repositórios **na mesma pasta**, o frontend com o nome `frontend`:
   ```
   git clone https://github.com/knowflow-tcc/knowflow-backend.git
   git clone https://github.com/knowflow-tcc/knowflow-frontend.git frontend
   ```
2. Dentro de `knowflow-backend`:
   - normal: `mvn spring-boot:run`
   - com dados de demonstração (senha `Knowflow2026`):
     `mvn spring-boot:run "-Dspring-boot.run.arguments=--knowflow.demo=true"`
   - testes automatizados: `mvn test`
3. Acesse http://localhost:8080 (Swagger em http://localhost:8080/swagger-ui.html).

As senhas do `application.properties` (banco `postgres` e admin inicial `Admin1234`)
são só para desenvolvimento local.

## Documentação
- `docs/banco/` - script do banco e backup
- `docs/postman/` - coleção do Postman com os casos de teste
- `docs/rastreabilidade.md` - requisitos x código
- `docs/guias/` - instalação, Postman/Swagger e GitHub
```

## 3. Testar antes de subir

Clone o frontend com o nome `frontend` (é dele que o backend lê as telas):

```powershell
cd C:\knowflow
git clone https://github.com/knowflow-tcc/knowflow-frontend.git frontend
cd C:\knowflow\knowflow-backend
mvn test
```

Tem que terminar com `Tests run: 111, Failures: 0, Errors: 0` e `BUILD SUCCESS`.
Se quiser, rode também `mvn spring-boot:run` e entre em http://localhost:8080
(Ctrl+C para parar). Depois apague o build para não confundir:

```powershell
Remove-Item -Recurse -Force target
```

## 4. Primeiro envio (branch main)

```powershell
git init -b main
git add .
git status
```

No `git status` devem aparecer **só**: `.gitignore`, `README.md`, `pom.xml`, `src/` e `docs/`.
Se aparecer `target/`, o `.gitignore` não foi salvo direito (volte ao 2.1).
Se estiver certo:

```powershell
git commit -m "Backend do KnowFlow: API REST, testes e documentação"
git remote add origin https://github.com/knowflow-tcc/knowflow-backend.git
git push -u origin main
```

## 5. Criar a branch develop

```powershell
git checkout -b develop
git push -u origin develop
```

## 6. Conferir

Abra https://github.com/knowflow-tcc/knowflow-backend: o README tem que aparecer na
página inicial e devem existir as branches `main` e `develop`.

## Problemas comuns

| Mensagem | O que fazer |
|---|---|
| `mvn` não é reconhecido | feche e abra o PowerShell; se continuar, o Maven não está no PATH |
| `remote origin already exists` | `git remote set-url origin https://github.com/knowflow-tcc/knowflow-backend.git` |
| `rejected ... fetch first` | o repositório não nasceu vazio. `git pull origin main --allow-unrelated-histories` e depois `git push -u origin main` |
| `Permission denied` / 403 | sua conta não tem permissão de escrita na organização |
| `not a git repository` | você não está na pasta: `cd C:\knowflow\knowflow-backend` |
| `destination path 'frontend' already exists` | o clone do passo 3 já foi feito; siga em frente |
