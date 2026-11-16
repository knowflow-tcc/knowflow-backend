# Subir o frontend no GitHub (repositório knowflow-frontend)

Segue a seção 7.1 do TCC: organização `knowflow-tcc` com dois repositórios,
`knowflow-frontend` (este guia) e `knowflow-backend` (depois).

A pasta `frontend` do projeto **não sai do lugar**: o backend lê as telas de `../frontend`.
Vamos subir uma **cópia** dela, numa pasta separada.

## 1. No site do GitHub

1. Entre em https://github.com com a sua conta.
2. Se a organização ainda não existe: clique no **+** (canto superior direito) → **New organization**
   → plano **Free** → nome `knowflow-tcc` → e-mail de contato → **Next**.
   Na tela seguinte, convide Danilo, Ícaro e Lucas (ou pule e convide depois em
   *Settings → Members*).
3. Dentro da organização: **New repository**
   - Owner: `knowflow-tcc`
   - Repository name: `knowflow-frontend`
   - Description: `Frontend do KnowFlow - HTML, CSS e JavaScript`
   - **Public**
   - **NÃO** marque "Add a README", nem .gitignore, nem licença (o repositório tem que nascer vazio)
   - **Create repository**

## 2. No PowerShell (copiar o frontend para uma pasta separada)

```powershell
New-Item -ItemType Directory -Force C:\knowflow
Copy-Item -Recurse "C:\Users\Breno\OneDrive\Documentos\TCC\fluxo-conhecimento\frontend" C:\knowflow\knowflow-frontend
cd C:\knowflow\knowflow-frontend
```

Crie o `README.md` (abre o Bloco de Notas; cole o texto abaixo, salve e feche):

```powershell
notepad README.md
```

```markdown
# KnowFlow - Frontend

Telas do sistema KnowFlow (TCC - UCSal): busca de conhecimento, registro de problemas,
causas e soluções, categorias em árvore, painel de indicadores e administração.

Feito com HTML, CSS, JavaScript e Bootstrap. As telas consomem a API REST do
repositório `knowflow-backend`.

## Como rodar
1. Clone este repositório com o nome `frontend`, na mesma pasta do backend:
   `git clone https://github.com/knowflow-tcc/knowflow-frontend.git frontend`
2. Rode o backend (ele serve estas telas em http://localhost:8080).
```

## 3. Primeiro envio (branch main)

Só na primeira vez no computador (troque pelo seu nome e e-mail do GitHub):

```powershell
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
```

Depois:

```powershell
git init -b main
git add .
git status
```

No `git status` devem aparecer: `README.md`, os `.html`, e as pastas `css/`, `js/`, `img/` e `error/`.
Se estiver certo:

```powershell
git commit -m "Frontend do KnowFlow: telas, estilos e scripts"
git remote add origin https://github.com/knowflow-tcc/knowflow-frontend.git
git push -u origin main
```

Na primeira vez vai abrir uma janela pedindo login no GitHub: entre e autorize.

## 4. Criar a branch develop (fluxo do TCC)

```powershell
git checkout -b develop
git push -u origin develop
```

## 5. Conferir

Abra https://github.com/knowflow-tcc/knowflow-frontend. Os arquivos e o README têm que
aparecer, e no seletor de branches têm que existir `main` e `develop`.

## Problemas comuns

| Mensagem | O que fazer |
|---|---|
| `remote origin already exists` | `git remote set-url origin https://github.com/knowflow-tcc/knowflow-frontend.git` |
| `rejected ... fetch first` | o repositório não nasceu vazio (marcou README no site). Rode `git pull origin main --allow-unrelated-histories`, depois `git push -u origin main` |
| `Permission denied` / 403 | a sua conta não é dona/membro da organização com permissão de escrita |
| `not a git repository` | você não está na pasta: `cd C:\knowflow\knowflow-frontend` |
