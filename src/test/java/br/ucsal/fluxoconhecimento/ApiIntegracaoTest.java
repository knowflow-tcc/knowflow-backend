package br.ucsal.fluxoconhecimento;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.CategoriaRepository;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// Testes da API com a aplicação completa (H2 em memória)
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegracaoTest {

    private static final String SENHA = "senhaTeste1";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DepartamentoRepository departamentoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long idTi;
    private Long idInfra;
    private Long idRedes;

    @BeforeAll
    void prepararDados() {
        Departamento ti = departamentoRepository.save(new Departamento("TI", null));
        Departamento rh = departamentoRepository.save(new Departamento("RH", null));
        idTi = ti.getId();
        criarUsuario("colab@empresa.com", Perfil.COLABORADOR, ti, true);
        criarUsuario("colega@empresa.com", Perfil.COLABORADOR, ti, true);
        criarUsuario("gestor.ti@empresa.com", Perfil.GESTOR, ti, true);
        criarUsuario("gestor.rh@empresa.com", Perfil.GESTOR, rh, true);
        criarUsuario("desligado@empresa.com", Perfil.COLABORADOR, ti, false);
        Categoria infra = categoriaRepository.save(new Categoria("Infraestrutura", null));
        idInfra = infra.getId();
        idRedes = categoriaRepository.save(new Categoria("Redes e Internet", infra)).getId();
    }

    private void criarUsuario(String email, Perfil perfil, Departamento departamento, boolean ativo) {
        Usuario u = new Usuario(email, email, passwordEncoder.encode(SENHA), perfil, departamento);
        u.setAtivo(ativo);
        usuarioRepository.save(u);
    }

    // ------------------------------------------------------------------ auxiliares

    private MockHttpSession login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private String corpo(Object objeto) throws Exception {
        return json.writeValueAsString(objeto);
    }

    private Map<String, Object> problema(String titulo, String descricao, Long categoriaId, List<String> palavras) {
        return Map.of("titulo", titulo, "descricao", descricao, "departamentoId", idTi,
                "categoriaId", categoriaId, "palavrasChave", palavras);
    }

    private long registrarProblema(MockHttpSession sessao, Map<String, Object> dados) throws Exception {
        MvcResult r = mvc.perform(post("/api/problemas").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(dados)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long registrarSolucao(MockHttpSession sessao, long idProblema, String texto) throws Exception {
        MvcResult r = mvc.perform(post("/api/problemas/" + idProblema + "/solucoes").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("descricao", texto))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    // ------------------------------------------------------------------ Quadro 8 - casos de teste

    @Test
    void ct01LoginComDadosValidos() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);

        mvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("colab@empresa.com"))
                .andExpect(jsonPath("$.perfil").value("COLABORADOR"));
    }

    @Test
    void ct02LoginComSenhaIncorreta() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", "colab@empresa.com", "senha", "errada123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("E-mail ou senha incorretos")));
    }

    @Test
    void ct03RegistrarProblemaEleApareceNaLista() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Monitor piscando na recepção",
                "O monitor da recepção pisca a cada poucos segundos", idInfra, List.of("monitor")));

        mvc.perform(get("/api/busca").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem((int) id)));
    }

    @Test
    void ct04RegistrarProblemaSemTituloMostraCampoObrigatorio() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);

        mvc.perform(post("/api/problemas").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(problema("", "Sem título", idInfra, List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").value("Informe o título do problema."));
    }

    @Test
    void ct05AssociarCausaESolucaoAUmProblema() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Teclado sem resposta", "Teclado USB não responde",
                idInfra, List.of()));

        mvc.perform(post("/api/problemas/" + id + "/causas").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("descricao", "Porta USB com defeito"))))
                .andExpect(status().isCreated());
        registrarSolucao(sessao, id, "Trocar o teclado de porta USB");

        mvc.perform(get("/api/problemas/" + id).session(sessao))
                .andExpect(jsonPath("$.causas[0].descricao").value("Porta USB com defeito"))
                .andExpect(jsonPath("$.solucoes[0].descricao").value("Trocar o teclado de porta USB"));
    }

    @Test
    void ct06BuscarPorPalavraChaveExistente() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Sistema de ponto fora do ar",
                "Ninguém consegue bater o ponto", idInfra, List.of("ponto eletrônico")));

        mvc.perform(get("/api/busca").param("texto", "ponto eletronico").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void ct07CadastrarProblemaParecidoSugereOSemelhante() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Impressora do financeiro não imprime",
                "Documentos ficam parados na fila de impressão", idInfra, List.of("impressora")));

        mvc.perform(get("/api/busca/semelhantes").param("texto", "impressora não imprime").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].semelhanca").value(100));
    }

    @Test
    void ct08GestorValidaSolucaoEProblemaFicaResolvido() throws Exception {
        MockHttpSession colab = login("colab@empresa.com", SENHA);
        long id = registrarProblema(colab, problema("VPN não conecta", "Erro 809 ao conectar", idRedes, List.of("vpn")));
        long solucao = registrarSolucao(colab, id, "Liberar a porta UDP 500 no roteador");

        MockHttpSession gestor = login("gestor.ti@empresa.com", SENHA);
        mvc.perform(patch("/api/solucoes/" + solucao + "/validar").session(gestor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validada").value(true));

        // RN08 - o problema passou automaticamente para "resolvido"
        mvc.perform(get("/api/problemas/" + id).session(colab))
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.historico[*].acao").value(hasItem("VALIDACAO")));
    }

    @Test
    void ct09ColaboradorNaoAcessaAAdministracao() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);

        mvc.perform(get("/api/usuarios").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios.html").session(sessao)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ regras de negócio e segurança

    @Test
    void rn01UsuarioDesativadoNaoEntra() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", "desligado@empresa.com", "senha", SENHA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("desativado")));
    }

    @Test
    void rnf01SemLoginAApiResponde401EATelaVaiParaOLogin() throws Exception {
        mvc.perform(get("/api/busca")).andExpect(status().isUnauthorized());
        mvc.perform(get("/index.html")).andExpect(redirectedUrlPattern("**/login.html"));
    }

    @Test
    void requisicaoSemTokenCsrfERecusada() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);

        mvc.perform(post("/api/problemas").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(problema("Sem CSRF", "Teste", idInfra, List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rn06ColaboradorNaoEditaProblemaDeOutraPessoaMasOGestorDoDepartamentoSim() throws Exception {
        MockHttpSession autor = login("colab@empresa.com", SENHA);
        Map<String, Object> dados = problema("Wi-Fi caindo no 2º andar", "A rede cai toda hora", idRedes, List.of());
        long id = registrarProblema(autor, dados);

        mvc.perform(put("/api/problemas/" + id).session(login("colega@empresa.com", SENHA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(dados)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/problemas/" + id).session(login("gestor.ti@empresa.com", SENHA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(dados)))
                .andExpect(status().isOk());
    }

    @Test
    void rn07GestorDeOutroDepartamentoNaoValida() throws Exception {
        MockHttpSession colab = login("colab@empresa.com", SENHA);
        long id = registrarProblema(colab, problema("E-mail não sincroniza", "Outlook parado", idInfra, List.of()));
        long solucao = registrarSolucao(colab, id, "Recriar o perfil do Outlook");

        mvc.perform(patch("/api/solucoes/" + solucao + "/validar").session(login("gestor.rh@empresa.com", SENHA)).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(containsString("TI")));
    }

    @Test
    void rn09Rn10ArquivarTiraDaBuscaEFicaNoHistorico() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Projetor sem imagem", "Projetor da sala 3 não liga",
                idInfra, List.of("projetor")));

        mvc.perform(patch("/api/problemas/" + id + "/arquivar").session(sessao).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(get("/api/busca").param("texto", "projetor").session(sessao))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) id))));
        MvcResult r = mvc.perform(get("/api/problemas/" + id).session(sessao)).andReturn();
        JsonNode historico = json.readTree(r.getResponse().getContentAsString()).get("historico");
        org.junit.jupiter.api.Assertions.assertEquals("ARQUIVAMENTO", historico.get(0).get("acao").asText());
        org.junit.jupiter.api.Assertions.assertEquals("INCLUSAO", historico.get(historico.size() - 1).get("acao").asText());
    }

    @Test
    void rf19FiltroPorCategoriaIncluiAsSubcategorias() throws Exception {
        MockHttpSession sessao = login("colab@empresa.com", SENHA);
        long id = registrarProblema(sessao, problema("Cabo de rede rompido", "Sem internet na sala 5",
                idRedes, List.of()));

        // O problema está em "Redes e Internet", filha de "Infraestrutura"
        mvc.perform(get("/api/busca").param("categoriaId", idInfra.toString()).session(sessao))
                .andExpect(jsonPath("$[*].id").value(hasItem((int) id)));
        mvc.perform(get("/api/busca").param("categoriaId", idInfra.toString()).param("status", "RESOLVIDO").session(sessao))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) id))));
    }

    @Test
    void rnf09AdministradorCadastraUsuarioComSenhaForte() throws Exception {
        MockHttpSession admin = login("admin@knowflow.com", "Admin1234");
        Map<String, Object> novo = Map.of("nome", "Novo Colaborador", "email", "novo@empresa.com",
                "perfil", "COLABORADOR", "departamentoId", idTi, "senha", "fraca");

        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(novo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos.senha").exists());
    }
}
