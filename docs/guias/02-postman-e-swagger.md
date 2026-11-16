# Testar a API: Postman e Swagger

O TCC cita o **Postman** como ferramenta de teste da API (seções 4.1 e 5.1). O projeto também
tem o **Swagger**, uma página dentro do próprio sistema que lista todos os endpoints e
deixa testá-los pelo navegador.

| | Postman | Swagger |
|---|---|---|
| O que é | Programa instalado no computador | Página web do próprio sistema |
| Onde | <https://www.postman.com/downloads/> | <http://localhost:8080/swagger-ui.html> |
| Para que usamos | Rodar os casos de teste CT01 a CT09 com verificação automática (evidência para o cap. 5) | Mostrar na banca, ao vivo, todos os endpoints da API |

> **Insomnia** faz o mesmo que o Postman. Como o TCC já cita o Postman, use só ele.

---

## Postman

### Importar a coleção (uma vez)

1. Instale o Postman e abra (pode usar sem criar conta: *Skip / Continue without an account*,
   ou crie uma conta gratuita).
2. **File > Import** (ou arraste o arquivo para a janela) e escolha
   [`docs/postman/KnowFlow.postman_collection.json`](../postman/KnowFlow.postman_collection.json).
3. Aparece a coleção **KnowFlow - Fluxo do Conhecimento**, com 4 pastas:

| Pasta | O que faz |
|---|---|
| 0 - Preparação | Entra como administrador e cria um departamento, um colaborador, um gestor e uma categoria só para o teste |
| 1 - Casos de teste do TCC | CT01 a CT09 do Quadro 8, cada um com verificação automática |
| 2 - Requisitos desejáveis | Comentário, "me ajudou", PDF, notificações, favoritos, anexo e recuperação de senha |
| 3 - Referência | Uma requisição para cada endpoint da API |

### Rodar tudo de uma vez (evidência para o capítulo 5)

1. Suba o sistema (`mvn spring-boot:run` na pasta `backend`).
2. Passe o mouse na coleção > **...** > **Run collection**.
3. Desmarque a pasta **3 - Referência**. Ela altera e arquiva registros e serve para uso manual.
4. Clique em **Run KnowFlow**.
5. Cada requisição mostra os testes **PASS/FAIL**. Tire um print dessa tela: é a evidência
   "respostas das requisições no Postman" que a seção 5.4 do TCC promete.

> A requisição **RF16 - Anexar arquivo** precisa que você escolha um arquivo. Abra ela, vá em
> *Body > form-data* e clique em *Select Files* no campo `arquivo`. No Runner, se o arquivo
> não for escolhido, só ela e a seguinte (baixar o anexo) falham.

### Rodar uma requisição por vez

Clique na requisição > **Send**. O resultado aparece embaixo: *Body* (resposta), *Test
Results* (verificações) e o código HTTP (200, 201, 400, 403...).

### Como a coleção funciona por dentro (para explicar na banca)

- **Sessão:** o login guarda um cookie (`JSESSIONID`) que o Postman reenvia sozinho. É a mesma
  coisa que o navegador faz.
- **CSRF:** toda requisição que altera dados (POST, PUT, PATCH, DELETE) precisa do cabeçalho
  `X-XSRF-TOKEN`. O script da coleção (*Pre-request Script*) busca esse token em
  `/api/auth/csrf` automaticamente antes de cada uma.
- **Variáveis:** `{{baseUrl}}`, `{{idProblema}}` etc. ficam na aba *Variables* da coleção. Os
  testes preenchem os ids conforme os registros são criados.

---

## Swagger

1. Com o sistema rodando, entre em <http://localhost:8080> e faça login (com qualquer perfil).
2. Abra <http://localhost:8080/swagger-ui.html>.
3. Os endpoints aparecem agrupados por controller. Clique em um > **Try it out** > preencha >
   **Execute**.

O Swagger usa a sessão do navegador e envia o token CSRF sozinho
(`springdoc.swagger-ui.csrf.enabled=true`). Por isso é preciso entrar no sistema antes. As
permissões continuam valendo: um colaborador que tentar `GET /api/usuarios` recebe **403**.

A descrição da API em formato OpenAPI (JSON) fica em <http://localhost:8080/v3/api-docs>.

---

## Códigos de resposta da API

| Código | Quando |
|---|---|
| 200 / 201 / 204 | Deu certo (201 = criado; 204 = deu certo e não há conteúdo de volta) |
| 400 | Campo obrigatório vazio ou formato inválido. `campos` diz qual campo corrigir |
| 401 | Não está logado, a sessão expirou (30 min - RNF10) ou o login falhou |
| 403 | O perfil não tem permissão (RF12) ou o token CSRF está faltando |
| 404 | Registro não encontrado |
| 413 | Arquivo maior que 5 MB |
| 422 | Regra de negócio (ex.: validar solução de outro departamento, link de senha expirado) |

Toda resposta de erro tem o mesmo formato (classe `TratadorGlobalErros`, RNF13):

```json
{ "status": 400, "mensagem": "Alguns campos precisam ser corrigidos.",
  "campos": { "titulo": "Informe o título do problema." }, "dataHora": "..." }
```
