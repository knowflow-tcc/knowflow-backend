# O que cada perfil faz hoje (e sugestões)

Retrato do sistema em 09/11/2026, tirado das regras do código (`SecurityConfig`, `Permissoes`
e o menu do `app.js`). O sistema tem **três perfis**: Colaborador, Gestor e Administrador
(Tabela 1 do TCC). Cada usuário tem um único perfil e pertence a um único departamento (RN03).

## Resumo em uma tabela

| Funcionalidade | Colaborador | Gestor | Administrador |
|---|:---:|:---:|:---:|
| Entrar, sair, recuperar a senha por e-mail | ✔ | ✔ | ✔ |
| Buscar problemas e soluções (com filtros) | ✔ | ✔ | ✔ |
| Ver os detalhes, o histórico e os anexos de um problema | ✔ | ✔ | ✔ |
| Marcar favoritos e usar "Meus favoritos" | ✔ | ✔ | ✔ |
| Receber notificações (sino) | ✔ | ✔ | (só se for autor de solução; na prática, não) |
| Registrar problema, causa e solução | ✔ | ✔ | ✘ |
| Ver sugestão de problemas semelhantes ao registrar | ✔ | ✔ | ✘ (não registra) |
| Editar, arquivar e mudar a situação **dos próprios** registros | ✔ | ✔ | ✘ |
| Editar e arquivar registros **de outras pessoas do seu departamento** | ✘ | ✔ | ✘ |
| Comentar soluções | ✔ | ✔ | ✘ |
| Dizer se uma solução ajudou (não vale na própria) | ✔ | ✔ | ✘ |
| Anexar arquivos (onde pode editar) | ✔ | ✔ | ✘ |
| Validar solução como eficaz | ✘ | ✔ só do seu departamento | ✔ de qualquer departamento |
| Organizar categorias (criar, renomear, mover) | ✘ | ✔ | ✘ |
| Painel de indicadores | ✘ | ✔ só do seu departamento | ✔ todos, com filtro |
| Relatório em PDF | ✘ | ✔ só do seu departamento | ✔ todos, com filtro |
| Cadastrar, editar, desativar, reativar e anonimizar usuários | ✘ | ✘ | ✔ |
| Cadastrar e editar departamentos | ✘ | ✘ | ✔ |
| Histórico geral de ações do sistema | ✘ | ✘ | ✔ |

## Colaborador

Funcionário que registra o que aconteceu e procura soluções antes de começar um atendimento.

**Menu:** Buscar conhecimento · Registrar problema · Meus favoritos.

- **Busca (RF08, RF19):** pesquisa por palavras e filtra por categoria (com as subcategorias),
  departamento, situação e período. Ordena por relevância ou data. Cada resultado mostra o
  percentual de semelhança e se já existe solução validada.
- **Registrar problema (RF04, RF07, RF09):** título, descrição, departamento, categoria e
  palavras-chave. Enquanto digita, aparecem os problemas parecidos já registrados.
- **Causas e soluções (RF05, RF06):** adiciona em qualquer problema ativo, inclusive de outras
  pessoas.
- **Editar e arquivar (RF15, RN06):** só o que ele mesmo criou. Também muda a situação
  (aberto/resolvido) dos próprios problemas (RF23). Nada é apagado (RN09).
- **Comentar (RF17)** soluções e **avaliar (RF18)** se uma solução ajudou. Não pode avaliar a
  própria.
- **Anexar (RF16)** imagens e documentos (até 5 MB) nos próprios problemas e soluções.
- **Favoritos (RF24)** e **notificações (RF21)**: é avisado quando a solução dele é validada ou
  comentada.

**Não pode:** validar soluções, editar registros de outras pessoas, ver o painel, gerar o
relatório, mexer em categorias ou em usuários.

## Gestor

Responsável por um departamento. Tem **tudo do colaborador**, mais:

**Menu:** o do colaborador + Painel de indicadores · Categorias.

- **Validar soluções (RF10, RN07)** dos problemas do **seu** departamento. O problema passa
  sozinho para "resolvido" (RN08) e o autor é notificado (RF21).
- **Editar e arquivar** problemas, causas, soluções, comentários e anexos de qualquer pessoa
  do **seu** departamento (RN06).
- **Categorias (RF07):** cria, renomeia e move categorias e subcategorias. A árvore impede que
  uma categoria seja colocada dentro dela mesma.
- **Painel (RF13):** 6 indicadores, 5 rankings e a evolução mensal, só do seu departamento.
- **Relatório em PDF (RF22)** por período, só do seu departamento.

**Não pode:** validar soluções de outros departamentos, gerenciar usuários ou departamentos,
ver o histórico geral.

## Administrador

Cuida da plataforma: usuários, departamentos e permissões.

**Menu:** Buscar conhecimento · Meus favoritos · Painel de indicadores · Usuários ·
Departamentos · Histórico de ações.

- **Usuários (RF02, RF12):** cadastra (com perfil e departamento), edita, troca a senha,
  desativa e reativa. O sistema não deixa o último administrador ativo ser desativado ou
  rebaixado.
- **Anonimizar (RNF02, LGPD):** apaga nome e e-mail de um usuário desativado, mantendo a autoria
  do conhecimento que ele registrou.
- **Departamentos (RF03):** cadastra e edita.
- **Histórico geral (RF11):** as últimas 300 ações do sistema, com filtro por tipo de registro.
- **Painel e relatório em PDF** de todos os departamentos, com filtro.
- **Valida soluções** de qualquer departamento.
- Consulta tudo: busca, detalhes e favoritos.

**Não pode:** registrar, editar ou comentar conhecimento (problemas, causas, soluções), nem
organizar categorias. Foi uma decisão para seguir o Quadro 2 do TCC, onde RF04 a RF07 citam só
Colaborador e Gestor.

## Pontos que podem surpreender na apresentação

1. **O administrador não organiza categorias.** O caso de uso "Gerenciar categorias" (Figura 3)
   é do Gestor. Se a banca perguntar "e se a empresa não tiver gestor?", hoje não há resposta.
2. **O administrador não registra problemas.** Segue o Quadro 2, mas pode parecer estranho
   numa demonstração. Para mostrar o registro, entre como colaborador.
3. **Uma validação não pode ser desfeita.** Se o gestor validar a solução errada, não há botão
   para tirar a validação.
4. **Registro arquivado não volta.** Arquivar é definitivo pela tela. Os dados continuam no
   banco (RN09), mas não existe "restaurar".

---

## Sugestões

Ordem: primeiro o que mais ajuda na banca e dá menos trabalho. **Toda funcionalidade nova
precisa de um RF novo no Overleaf.** Por isso, em cada item vai o texto sugerido. Antes de
implementar, me diga quais vocês querem.

### Prioridade alta (rápidas e fecham buracos visíveis)

| # | Sugestão | Perfil | Por quê | Texto de RF sugerido |
|---|---|---|---|---|
| 1 | **"Minha conta":** trocar a própria senha estando logado | Todos | Hoje só dá pelo "esqueci a senha" ou pedindo ao administrador. A banca costuma testar isso | O sistema deve permitir que o usuário altere a própria senha, informando a senha atual. |
| 2 | **Desfazer validação** de uma solução | Gestor, Administrador | Corrige um clique errado. O problema volta para "aberto" se não sobrar solução validada | O sistema deve permitir que o gestor remova a validação de uma solução, registrando a ação no histórico. |
| 3 | **Fila "Aguardando validação"** no painel do gestor | Gestor | Mostra as soluções do departamento que ainda não foram validadas. Deixa o papel do gestor mais claro na demonstração | O sistema deve exibir ao gestor as soluções do seu departamento pendentes de validação. |
| 4 | **"Meus registros"** no menu | Colaborador, Gestor | Lista o que a pessoa registrou, com a situação de cada problema | O sistema deve permitir que o usuário consulte os problemas e soluções que registrou. |
| 5 | **Restaurar registro arquivado** | Gestor, Administrador | Complementa a RN09: se nada é apagado, faz sentido poder trazer de volta | O sistema deve permitir restaurar registros arquivados, registrando a ação no histórico. |

### Prioridade média

| # | Sugestão | Perfil | Por quê |
|---|---|---|---|
| 6 | **Administrador também organiza categorias** | Administrador | Resolve o caso "empresa sem gestor". Precisa atualizar o caso de uso da Figura 3 |
| 7 | **Notificar o autor do problema** quando chega uma solução nova ou o problema é resolvido | Colaborador, Gestor | Hoje só o autor da solução é avisado. Quem abriu o problema também quer saber |
| 8 | **Bloquear o login após várias tentativas erradas** (ex.: 5 tentativas, 15 minutos) | Todos | Reforça o RNF01 contra tentativa de adivinhar senha. Bom argumento de segurança na banca |
| 9 | **Exportar os dados pessoais de um usuário** (LGPD, direito de acesso) | Administrador | Completa o RNF02, que hoje tem desativação e anonimização |
| 10 | **Comentar o problema** (não só as soluções) | Colaborador, Gestor | Para perguntar algo antes de existir solução |

### Para o futuro (combinam com as seções 6.5 e 7 do TCC)

| # | Sugestão | Observação |
|---|---|---|
| 11 | Envio real de e-mail (Gmail ou outro SMTP) | Já está pronto no código: basta preencher `spring.mail.*` no `application.properties` |
| 12 | Busca por sinônimos ("lento" = "travando") | É a limitação da 6.5. Um dicionário simples de sinônimos já melhora |
| 13 | Teste de carga para comprovar o RNF04 (3 segundos) | A 6.5 diz que não foi medido. Dá para medir com o JMeter |
| 14 | Validação com usuários reais (seção 5.6) | Um questionário curto com 3 a 5 pessoas já transforma "parcialmente atingido" em evidência |

### Minha recomendação

Se der tempo para só três, faça **1 (Minha conta)**, **2 (desfazer validação)** e
**3 (fila de validação)**. São rápidas, aparecem bem na demonstração e respondem perguntas que
a banca costuma fazer. A **14 (validação com usuários)** não é código, mas é o único objetivo
que o próprio TCC marca como "parcialmente atingido".
