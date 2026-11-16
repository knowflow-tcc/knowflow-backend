package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import br.ucsal.fluxoconhecimento.Fabrica;
import br.ucsal.fluxoconhecimento.dto.ProblemaDetalheDTO;
import br.ucsal.fluxoconhecimento.dto.ProblemaFormDTO;
import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.PalavraChave;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.AnexoRepository;
import br.ucsal.fluxoconhecimento.repository.FavoritoRepository;
import br.ucsal.fluxoconhecimento.repository.PalavraChaveRepository;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;

// RF04, RF07, RF15 e RF23 com as regras RN06, RN09 e RN10
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProblemaServiceTest {

    @Mock private ProblemaRepository problemaRepository;
    @Mock private PalavraChaveRepository palavraChaveRepository;
    @Mock private DepartamentoService departamentoService;
    @Mock private CategoriaService categoriaService;
    @Mock private BuscaService buscaService;
    @Mock private HistoricoService historicoService;
    @Mock private UsuarioLogadoService usuarioLogado;
    @Mock private AnexoRepository anexoRepository;
    @Mock private FavoritoRepository favoritoRepository;
    @InjectMocks private ProblemaService service;

    private Departamento ti;
    private Categoria redes;
    private Usuario autor;
    private Usuario outroColaborador;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        ti = Fabrica.departamento(1, "TI");
        redes = Fabrica.categoria(7, "Redes", null);
        autor = Fabrica.usuario(1, Perfil.COLABORADOR, ti);
        outroColaborador = Fabrica.usuario(2, Perfil.COLABORADOR, ti);
        admin = Fabrica.usuario(3, Perfil.ADMINISTRADOR, ti);

        when(usuarioLogado.obter(1L)).thenReturn(autor);
        when(usuarioLogado.obter(2L)).thenReturn(outroColaborador);
        when(usuarioLogado.obter(3L)).thenReturn(admin);
        when(departamentoService.buscarEntidade(1L)).thenReturn(ti);
        when(categoriaService.buscarEntidade(7L)).thenReturn(redes);
        when(categoriaService.arvore()).thenReturn(
                new ArvoreCategorias(List.of(new ArvoreCategorias.Entrada(7, "Redes", null))));
        when(historicoService.doProblema(any(), any())).thenReturn(List.of());
        when(palavraChaveRepository.findByTermo(anyString())).thenReturn(Optional.empty());
        when(palavraChaveRepository.save(any(PalavraChave.class))).thenAnswer(inv -> inv.getArgument(0));
        when(problemaRepository.save(any(Problema.class))).thenAnswer(inv -> {
            Problema p = inv.getArgument(0);
            p.setId(50L);
            return p;
        });
    }

    private ProblemaFormDTO form(List<String> palavras) {
        return new ProblemaFormDTO("VPN não conecta", "Erro 809 ao conectar de casa", 1L, 7L, palavras);
    }

    @Test
    void registrarUsaOUsuarioLogadoComoAutorEComecaAberto() {
        ProblemaDetalheDTO dto = service.registrar(form(List.of()), 1L);

        assertEquals("Usuário 1", dto.autorNome());
        assertEquals(StatusProblema.ABERTO.name(), dto.status());
        verify(historicoService).registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_PROBLEMA, 50L, autor);
        verify(buscaService).indexar(any(Problema.class));
    }

    @Test
    void rf07PalavrasChaveSaoNormalizadasESemRepeticao() {
        ProblemaDetalheDTO dto = service.registrar(form(List.of(" VPN ", "vpn", "Home  Office", "")), 1L);

        assertEquals(List.of("vpn", "home office"), dto.palavrasChave());
    }

    @Test
    void administradorNaoRegistraProblemas() {
        assertThrows(AcessoNegadoException.class, () -> service.registrar(form(List.of()), 3L));
        verify(problemaRepository, never()).save(any());
    }

    @Test
    void rn06ColaboradorNaoEditaProblemaDeOutraPessoa() {
        Problema problema = Fabrica.problema(50, autor, ti, redes);
        when(problemaRepository.findById(50L)).thenReturn(Optional.of(problema));

        assertThrows(AcessoNegadoException.class, () -> service.atualizar(50L, form(List.of()), 2L));
    }

    @Test
    void rn09ArquivarTiraDaBuscaMasNaoExclui() {
        Problema problema = Fabrica.problema(50, autor, ti, redes);
        when(problemaRepository.findById(50L)).thenReturn(Optional.of(problema));

        service.arquivar(50L, 1L);

        assertFalse(problema.isAtivo());
        verify(buscaService).remover(50L);
        verify(problemaRepository, never()).delete(any());
        verify(historicoService).registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_PROBLEMA, 50L, autor);
    }

    @Test
    void problemaArquivadoNaoPodeSerEditado() {
        Problema problema = Fabrica.problema(50, autor, ti, redes);
        problema.setAtivo(false);
        when(problemaRepository.findById(50L)).thenReturn(Optional.of(problema));

        assertThrows(RegraNegocioException.class, () -> service.atualizar(50L, form(List.of()), 1L));
    }

    @Test
    void rf23AlterarSituacaoGeraHistorico() {
        Problema problema = Fabrica.problema(50, autor, ti, redes);
        when(problemaRepository.findById(50L)).thenReturn(Optional.of(problema));

        service.alterarStatus(50L, StatusProblema.RESOLVIDO, 1L);

        assertEquals(StatusProblema.RESOLVIDO, problema.getStatus());
        verify(historicoService).registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_PROBLEMA, 50L, autor);
    }
}
