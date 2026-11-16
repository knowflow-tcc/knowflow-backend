package br.ucsal.fluxoconhecimento;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.springframework.mock.web.MockMultipartFile;
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

// Testes de ponta a ponta dos requisitos desejáveis
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DesejaveisIntegracaoTest {

    private static final String SENHA = "senhaTeste1";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DepartamentoRepository departamentoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long idSuporte;
    private Long idCategoria;

    @BeforeAll
    void prepararDados() {
        Departamento suporte = departamentoRepository.save(new Departamento("Suporte", null));
        Departamento financeiro = departamentoRepository.save(new Departamento("Financeiro", null));
        idSuporte = suporte.getId();
        criarUsuario("autor@suporte.com", Perfil.COLABORADOR, suporte);
        criarUsuario("leitor@suporte.com", Perfil.COLABORADOR, suporte);
        criarUsuario("gestor@suporte.com", Perfil.GESTOR, suporte);
        criarUsuario("gestor@financeiro.com", Perfil.GESTOR, financeiro);
        criarUsuario("admin@suporte.com", Perfil.ADMINISTRADOR, suporte);
        idCategoria = categoriaRepository.save(new Categoria("Equipamentos", null)).getId();
    }

    private void criarUsuario(String email, Perfil perfil, Departamento departamento) {
        usuarioRepository.save(new Usuario(email, email, passwordEncoder.encode(SENHA), perfil, departamento));
    }

    // ------------------------------------------------------------------ auxiliares

    private MockHttpSession login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", email, "senha", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private String corpo(Object objeto) throws Exception {
        return json.writeValueAsString(objeto);
    }

    private JsonNode lerJson(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long registrarProblema(MockHttpSession sessao, String titulo) throws Exception {
        Map<String, Object> dados = Map.of("titulo", titulo, "descricao", "Descrição de " + titulo,
                "departamentoId", idSuporte, "categoriaId", idCategoria, "palavrasChave", List.of("teste"));
        MvcResult r = mvc.perform(post("/api/problemas").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(dados)))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(r).get("id").asLong();
    }

    private long registrarSolucao(MockHttpSession sessao, long idProblema) throws Exception {
        MvcResult r = mvc.perform(post("/api/problemas/" + idProblema + "/solucoes").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("descricao", "Reiniciar o equipamento"))))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(r).get("id").asLong();
    }

    private JsonNode detalhes(MockHttpSession sessao, long idProblema) throws Exception {
        return lerJson(mvc.perform(get("/api/problemas/" + idProblema).session(sessao))
                .andExpect(status().isOk()).andReturn());
    }

    private long naoLidas(MockHttpSession sessao) throws Exception {
        return lerJson(mvc.perform(get("/api/notificacoes").session(sessao))
                .andExpect(status().isOk()).andReturn()).get("naoLidas").asLong();
    }

    // ------------------------------------------------------------------ RF24 favoritos

    @Test
    void rf24MarcarEDesmarcarFavoritoEFiltrarNaBusca() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long id = registrarProblema(autor, "Monitor piscando favorito");

        mvc.perform(put("/api/problemas/" + id + "/favorito").session(autor).with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(detalhes(autor, id).get("favorito").asBoolean());
        mvc.perform(get("/api/busca").param("somenteFavoritos", "true").session(autor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem((int) id)))
                .andExpect(jsonPath("$[0].favorito").value(true));

        mvc.perform(delete("/api/problemas/" + id + "/favorito").session(autor).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/busca").param("somenteFavoritos", "true").session(autor))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) id))));
    }

    @Test
    void rf24AdministradorTambemUsaFavoritos() throws Exception {
        long id = registrarProblema(login("autor@suporte.com"), "Teclado sem resposta");
        MockHttpSession admin = login("admin@suporte.com");

        mvc.perform(put("/api/problemas/" + id + "/favorito").session(admin).with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(detalhes(admin, id).get("favorito").asBoolean());
    }

    // ------------------------------------------------------------------ RF17 comentários + RF21 notificações

    @Test
    void rf17ComentarSolucaoERf21NotificarOAutor() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        MockHttpSession leitor = login("leitor@suporte.com");
        long id = registrarProblema(autor, "Mouse falhando");
        long idSolucao = registrarSolucao(autor, id);
        long antes = naoLidas(autor);

        mvc.perform(post("/api/solucoes/" + idSolucao + "/comentarios").session(leitor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("texto", "Funcionou aqui também."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Funcionou aqui também."));

        assertEquals("Funcionou aqui também.",
                detalhes(leitor, id).get("solucoes").get(0).get("comentarios").get(0).get("texto").asText());
        assertEquals(antes + 1, naoLidas(autor));
        mvc.perform(get("/api/notificacoes").session(autor))
                .andExpect(jsonPath("$.itens[0].mensagem").value(containsString("comentou a sua solução")))
                .andExpect(jsonPath("$.itens[0].idProblema").value(id));

        mvc.perform(patch("/api/notificacoes/lidas").session(autor).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0, naoLidas(autor));
    }

    @Test
    void rf17ComentarioEmBrancoERecusado() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long idSolucao = registrarSolucao(autor, registrarProblema(autor, "Cabo solto"));

        mvc.perform(post("/api/solucoes/" + idSolucao + "/comentarios").session(autor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("texto", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.texto").value("Escreva o comentário."));
    }

    @Test
    void rf17AutorArquivaOProprioComentarioEOutroColaboradorNao() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        MockHttpSession leitor = login("leitor@suporte.com");
        long id = registrarProblema(autor, "Webcam escura");
        long idSolucao = registrarSolucao(autor, id);
        MvcResult r = mvc.perform(post("/api/solucoes/" + idSolucao + "/comentarios").session(leitor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("texto", "Verifique a tampa da lente."))))
                .andExpect(status().isCreated()).andReturn();
        long idComentario = lerJson(r).get("id").asLong();

        mvc.perform(patch("/api/comentarios/" + idComentario + "/arquivar").session(autor).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/comentarios/" + idComentario + "/arquivar").session(leitor).with(csrf()))
                .andExpect(status().isOk());
        assertEquals(0, detalhes(autor, id).get("solucoes").get(0).get("comentarios").size());
    }

    // ------------------------------------------------------------------ RF18 "me ajudou"

    @Test
    void rf18VotoMeAjudouETrocaDeVoto() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        MockHttpSession leitor = login("leitor@suporte.com");
        long idSolucao = registrarSolucao(autor, registrarProblema(autor, "Impressora lenta"));

        mvc.perform(put("/api/solucoes/" + idSolucao + "/avaliacao").session(leitor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("ajudou", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAjudou").value(1))
                .andExpect(jsonPath("$.minhaAvaliacao").value(true));

        mvc.perform(put("/api/solucoes/" + idSolucao + "/avaliacao").session(leitor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("ajudou", false))))
                .andExpect(jsonPath("$.totalAjudou").value(0))
                .andExpect(jsonPath("$.totalNaoAjudou").value(1));
    }

    @Test
    void rf18AutorNaoAvaliaAPropriaSolucao() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long idSolucao = registrarSolucao(autor, registrarProblema(autor, "Scanner travado"));

        mvc.perform(put("/api/solucoes/" + idSolucao + "/avaliacao").session(autor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("ajudou", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(containsString("você mesmo registrou")));
    }

    @Test
    void rf13PainelMostraAsSolucoesMaisUteis() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long idSolucao = registrarSolucao(autor, registrarProblema(autor, "Nobreak apitando"));
        mvc.perform(put("/api/solucoes/" + idSolucao + "/avaliacao").session(login("leitor@suporte.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("ajudou", true))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/painel").session(login("gestor@suporte.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solucoesMaisUteis[*].rotulo").value(hasItem(containsString("Nobreak apitando"))));
    }

    // ------------------------------------------------------------------ RF10 + RF21

    @Test
    void rf21AutorENotificadoQuandoASolucaoEValidada() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long idSolucao = registrarSolucao(autor, registrarProblema(autor, "Projetor sem imagem"));
        long antes = naoLidas(autor);

        mvc.perform(patch("/api/solucoes/" + idSolucao + "/validar").session(login("gestor@suporte.com")).with(csrf()))
                .andExpect(status().isOk());

        assertEquals(antes + 1, naoLidas(autor));
        mvc.perform(get("/api/notificacoes").session(autor))
                .andExpect(jsonPath("$.itens[0].mensagem").value(containsString("foi validada")));
    }

    // ------------------------------------------------------------------ RF16 anexos

    @Test
    void rf16AnexarBaixarEArquivarImagem() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long id = registrarProblema(autor, "Tela azul ao iniciar");
        byte[] imagem = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        MvcResult r = mvc.perform(multipart("/api/problemas/" + id + "/anexos")
                        .file(new MockMultipartFile("arquivo", "erro.png", "image/png", imagem))
                        .session(autor).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeArquivo").value("erro.png"))
                .andExpect(jsonPath("$.imagem").value(true))
                .andReturn();
        long idAnexo = lerJson(r).get("id").asLong();
        assertEquals(1, detalhes(autor, id).get("anexos").size());

        mvc.perform(get("/api/anexos/" + idAnexo).session(login("leitor@suporte.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(imagem));

        mvc.perform(patch("/api/anexos/" + idAnexo + "/arquivar").session(autor).with(csrf()))
                .andExpect(status().isNoContent());
        JsonNode depois = detalhes(autor, id);
        assertEquals(0, depois.get("anexos").size());
        assertTrue(depois.get("historico").toString().contains("anexo"), "RN10: o arquivamento entra no histórico");
    }

    @Test
    void rf16AnexarNaSolucao() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long id = registrarProblema(autor, "Erro na planilha de custos");
        long idSolucao = registrarSolucao(autor, id);

        mvc.perform(multipart("/api/solucoes/" + idSolucao + "/anexos")
                        .file(new MockMultipartFile("arquivo", "passo-a-passo.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .session(autor).with(csrf()))
                .andExpect(status().isCreated());

        assertEquals("passo-a-passo.pdf",
                detalhes(autor, id).get("solucoes").get(0).get("anexos").get(0).get("nomeArquivo").asText());
    }

    @Test
    void rf16TipoDeArquivoNaoPermitidoERecusado() throws Exception {
        MockHttpSession autor = login("autor@suporte.com");
        long id = registrarProblema(autor, "Programa não abre");

        mvc.perform(multipart("/api/problemas/" + id + "/anexos")
                        .file(new MockMultipartFile("arquivo", "virus.exe", "application/octet-stream", new byte[] {1}))
                        .session(autor).with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos.arquivo").value(containsString("não permitido")));
    }

    @Test
    void rf16Rn06ColaboradorNaoAnexaEmProblemaDeOutraPessoa() throws Exception {
        long id = registrarProblema(login("autor@suporte.com"), "Telefone mudo");

        mvc.perform(multipart("/api/problemas/" + id + "/anexos")
                        .file(new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", new byte[] {1, 2}))
                        .session(login("leitor@suporte.com")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ RF22 relatório em PDF

    @Test
    void rf22GestorGeraORelatorioEmPdf() throws Exception {
        registrarProblema(login("autor@suporte.com"), "Ar-condicionado pingando");
        String hoje = LocalDate.now().toString();

        MvcResult r = mvc.perform(get("/api/relatorios/problemas.pdf").param("de", hoje).param("ate", hoje)
                        .session(login("gestor@suporte.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();
        byte[] pdf = r.getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void rf22ColaboradorNaoGeraRelatorio() throws Exception {
        mvc.perform(get("/api/relatorios/problemas.pdf").session(login("autor@suporte.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf22PeriodoInvalidoTemMensagemClara() throws Exception {
        mvc.perform(get("/api/relatorios/problemas.pdf").param("de", "2026-05-10").param("ate", "2026-05-01")
                        .session(login("admin@suporte.com")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("data final não pode ser anterior")));
    }

    // ------------------------------------------------------------------ RF14 recuperação de senha

    @Test
    void rf14PedirRecuperacaoNaoExigeLoginENaoRevelaSeOEmailExiste() throws Exception {
        mvc.perform(post("/api/auth/recuperar-senha").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", "ninguem@lugar-nenhum.com"))))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/recuperar-senha").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", "autor@suporte.com"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void rf14LinkInvalidoERecusado() throws Exception {
        mvc.perform(post("/api/auth/redefinir-senha").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("token", "codigo-inventado", "senha", "novaSenha123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("inválido, já foi usado ou expirou")));
    }

    // ------------------------------------------------------------------ Dados iniciais

    @Test
    void administradorInicialComDepartamentoAcentuadoCorretamente() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(Map.of("email", "admin@knowflow.com", "senha", "Admin1234"))))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("Administração", lerJson(r).get("departamentoNome").asText());
    }

    // ------------------------------------------------------------------ Swagger

    @Test
    void documentacaoDaApiDisponivel() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value(containsString("KnowFlow")));
    }
}
