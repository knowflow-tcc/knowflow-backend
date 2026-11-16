# Rastreabilidade: requisito → código → teste

Onde cada requisito do TCC está implementado e qual teste automático o comprova. Use esta
tabela para responder à banca "mostre onde está o RFxx".

Pastas: `backend/src/main/java/br/ucsal/fluxoconhecimento/` (código) e
`backend/src/test/java/br/ucsal/fluxoconhecimento/` (testes). Telas em `frontend/`.

**Resultado atual:** 24 de 24 RFs, 14 de 14 RNFs e 10 de 10 RNs implementados, 15 tabelas,
10 telas (mais as páginas de erro 403 e 404), **111 testes automatizados aprovados** (`mvn test`).

## Requisitos funcionais

| RF | Requisito | Prioridade | Backend (principais classes) | Tela | Testes |
|---|---|---|---|---|---|
| RF01 | Autenticação por e-mail e senha | Essencial | `AuthController`, `AutenticacaoService`, `SecurityConfig` | login.html | `ct01LoginComDadosValidos`, `ct02LoginComSenhaIncorreta` |
| RF02 | Cadastro, edição e desativação de usuários | Essencial | `UsuarioController`, `UsuarioService` | usuarios.html | `UsuarioServiceTest`, `rnf09AdministradorCadastraUsuarioComSenhaForte` |
| RF03 | Departamentos e vínculo de usuários | Essencial | `DepartamentoController`, `DepartamentoService` | departamentos.html | `ApiIntegracaoTest` (dados) |
| RF04 | Registrar problema | Essencial | `ProblemaController.registrar`, `ProblemaService` | problema-form.html | `ct03…`, `ct04…`, `registrarUsaOUsuarioLogadoComoAutorEComecaAberto` |
| RF05 | Causas do problema | Essencial | `ProblemaController.registrarCausa`, `CausaService` | problema.html | `ct05AssociarCausaESolucaoAUmProblema` |
| RF06 | Soluções do problema | Essencial | `ProblemaController.registrarSolucao`, `SolucaoService` | problema.html | `ct05…`, `rn10RegistrarSolucaoGeraHistoricoEReindexa` |
| RF07 | Categorias e palavras-chave | Importante | `CategoriaService`, **`estrutura/ArvoreCategorias`**, `PalavraChave` | categorias.html, problema-form.html | `ArvoreCategoriasTest` (8), `CategoriaServiceTest` (5), `rf07PalavrasChave…` |
| RF08 | Busca por palavra-chave, categoria e departamento | Essencial | `BuscaService`, **`estrutura/IndiceBuscaHash`** (tabela hash) | index.html | `ct06BuscarPorPalavraChaveExistente`, `IndiceBuscaHashTest` (12) |
| RF09 | Sugerir problemas semelhantes | Importante | `BuscaService.semelhantes` | problema-form.html | `ct07CadastrarProblemaParecidoSugereOSemelhante` |
| RF10 | Gestor valida solução | Importante | `SolucaoService.validar` | problema.html | `ct08…`, `rn08…`, `rn07…` |
| RF11 | Histórico de alterações | Importante | `HistoricoService`, `Historico` | problema.html, historico.html | `rn09Rn10ArquivarTiraDaBuscaEFicaNoHistorico` |
| RF12 | Controle de acesso por perfil | Essencial | `SecurityConfig` (por URL) + `Permissoes` (por registro) | menu de cada tela | `ct09ColaboradorNaoAcessaAAdministracao`, `PermissoesTest` (6) |
| RF13 | Painel de indicadores | Importante | `PainelService` (usa `TreeMap` na evolução mensal) | painel.html | `rf13PainelMostraAsSolucoesMaisUteis` |
| RF14 | Recuperação de senha por e-mail | Desejável | `RecuperacaoSenhaService`, `EnvioEmailService`, `TokenRecuperacaoSenha` | recuperar-senha.html | `RecuperacaoSenhaServiceTest` (5), `rf14…` (2) |
| RF15 | Editar e arquivar problemas, causas e soluções | Essencial | `ProblemaService`, `CausaService`, `SolucaoService` | problema.html | `rn06…`, `rn09…`, `problemaArquivadoNaoPodeSerEditado` |
| RF16 | Anexos em problemas e soluções | Desejável | `AnexoService`, `AnexoController`, `Anexo` | problema.html | `rf16…` (4) |
| RF17 | Comentar soluções | Desejável | `ComentarioService`, `Comentario` | problema.html | `rf17…` (3) |
| RF18 | "Esta solução me ajudou" | Desejável | `AvaliacaoSolucaoService`, `AvaliacaoSolucao` | problema.html | `rf18…` (2) |
| RF19 | Filtrar e ordenar a busca | Importante | `BuscaService.buscar` (filtros e `Comparator`) | index.html | `rf19FiltroPorCategoriaIncluiAsSubcategorias` |
| RF20 | Detalhes do problema | Essencial | `ProblemaService.detalhar` → `ProblemaDetalheDTO` | problema.html | `ct08…` |
| RF21 | Notificar o autor (validação/comentário) | Desejável | `NotificacaoService`, `Notificacao` | sino no topo (app.js) | `rf17ComentarSolucaoERf21NotificarOAutor`, `rf21…` |
| RF22 | Relatório em PDF por período e departamento | Desejável | `RelatorioService` (OpenPDF), `RelatorioController` | painel.html | `rf22…` (3) |
| RF23 | Situação aberto/resolvido | Essencial | `ProblemaService.alterarStatus` | problema.html | `rf23AlterarSituacaoGeraHistorico` |
| RF24 | Favoritos | Desejável | `FavoritoService`, `Favorito`, filtro em `BuscaService` | index.html ("Meus favoritos"), problema.html | `rf24…` (2) |

## Requisitos não funcionais

| RNF | Como foi atendido | Onde |
|---|---|---|
| RNF01 Segurança | Senhas com hash **BCrypt**; toda rota exige login, exceto login e recuperação de senha | `SecurityConfig.passwordEncoder`, `rnf01SemLoginAApiResponde401…` |
| RNF02 Privacidade (LGPD) | Exclusão lógica (desativação) e **anonimização** de nome e e-mail de usuários desligados; link de senha guardado só como hash | `UsuarioService.anonimizar`, `lgpd…` (2 testes) |
| RNF03 Usabilidade | Busca na tela inicial, sugestão enquanto digita, atalho Ctrl+K | index.html, problema-form.html |
| RNF04 Desempenho (até 3 s) | Índice em tabela hash (tempo médio constante por termo); a tela mostra o tempo de cada busca | `IndiceBuscaHash`, index.html |
| RNF05 Compatibilidade | Bootstrap 5 responsivo; testado no Chrome e no Edge | todas as telas |
| RNF06 Manutenibilidade | Orientado a objetos, camadas Controller → Service → Repository → Model com DTOs; Git | estrutura de pacotes |
| RNF07 Integridade | Chaves estrangeiras em todas as relações; `CHECK` do anexo (problema **ou** solução) | `docs/banco/schema-postgresql.sql` |
| RNF08 Escalabilidade | API sem estado de tela, índice em memória reconstruído na inicialização, carga em lotes (`batch_fetch_size`) | `application.properties` |
| RNF09 Senha forte | Mínimo 8 caracteres, com letras e números (cadastro e recuperação) | `PoliticaSenha`, `PoliticaSenhaTest` (8) |
| RNF10 Sessão de 30 min | `server.servlet.session.timeout=30m` | `application.properties` |
| RNF11 Backup diário | Script `pg_dump` agendável no Windows (anexos incluídos, pois ficam no banco) | `docs/banco/backup-diario.bat` |
| RNF12 Português do Brasil | Telas, mensagens e nomes de classes em português | todo o projeto |
| RNF13 Mensagens claras | Erros com mensagem e campo a corrigir | `TratadorGlobalErros`, `ErroDTO` |
| RNF14 Tecnologias | Java 17 + Spring Boot 3.3, HTML/CSS/JS + Bootstrap 5, PostgreSQL 16 | `pom.xml`, `frontend/` |

## Regras de negócio

| RN | Onde é garantida | Teste |
|---|---|---|
| RN01 Só usuários autenticados e ativos | `UsuarioAutenticado.isEnabled`, `UsuarioLogadoService` | `rn01UsuarioDesativadoNaoEntra` |
| RN02 E-mail único | `unique` no banco + `UsuarioService` | `rn02EmailRepetidoERecusado` |
| RN03 Um departamento e um perfil | colunas obrigatórias em `usuario` | `UsuarioServiceTest` |
| RN04 Campos obrigatórios do problema | Bean Validation em `ProblemaFormDTO` | `ct04…` |
| RN05 Causa/solução só em problema existente | FK obrigatória + `SolucaoService` | `rn05NaoRegistraSolucaoEmProblemaArquivado` |
| RN06 Quem edita | `Permissoes.podeEditar` (também anexos e comentários) | `rn06…` (5 testes) |
| RN07 Quem valida | `Permissoes.podeValidar` | `rn07…` (3 testes) |
| RN08 Validada → resolvido | `SolucaoService.validar` | `rn08ValidarSolucaoMarcaOProblemaComoResolvido` |
| RN09 Arquivar, nunca excluir | campo `ativo` em problema, causa, solução, comentário, anexo e usuário | `rn09…` (3 testes) |
| RN10 Histórico de toda inclusão/alteração/arquivamento | `HistoricoService.registrar` em todos os serviços | `rn10…`, `rf16AnexarBaixarEArquivarImagem` |

## Estruturas de dados (seção 3.2)

| Estrutura | Classe | Uso |
|---|---|---|
| Tabela hash (`HashMap`) | `estrutura/IndiceBuscaHash` | Índice invertido: termo → problemas. Busca (RF08), semelhantes (RF09) e recorrentes do painel (RF13) |
| Árvore | `estrutura/ArvoreCategorias` | Categorias e subcategorias: caminho completo, busca incluindo subcategorias, bloqueio de ciclo |
| Árvore balanceada (`TreeMap`) | `PainelService.evolucaoMensal` | Mantém os meses em ordem cronológica no gráfico |

## Decisões tomadas durante o desenvolvimento (para a banca e para o Overleaf)

1. **O administrador não registra conhecimento.** RF04 a RF06 citam só Colaborador e Gestor.
   O administrador consulta, valida (RN07) e gerencia, mas não registra problemas, causas
   ou soluções.
2. **Categorias e departamentos não são excluídos**, só renomeados (RN09 e integridade dos
   problemas já registrados).
3. **Anonimização (LGPD):** o administrador pode anonimizar um usuário desativado. Nome e
   e-mail viram "Usuário anonimizado #id" e a autoria do conhecimento é preservada. Texto de
   RF sugerido em `docs/overleaf/alteracoes-para-o-overleaf.md`.
4. **Favoritos, votos e notificações não geram histórico.** São preferências pessoais, não
   registros de conhecimento. Comentários e anexos geram histórico (RN10).
5. **RF13 "soluções mais reutilizadas"** = soluções com mais votos "me ajudou" (RF18).
6. **Anexos ficam dentro do banco** (`bytea`), para entrarem no backup (RNF11). Limite de 5 MB
   e lista de tipos permitidos.
7. **RF14 sem servidor de e-mail:** enquanto o SMTP não é configurado, o link aparece no
   console da aplicação. Para envio real, basta preencher `spring.mail.*`.
8. **O autor não avalia a própria solução** (RF18), para o voto não ser enviesado.
