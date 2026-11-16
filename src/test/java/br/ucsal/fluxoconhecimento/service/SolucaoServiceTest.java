package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;

// RF06, RF10 e as regras RN05, RN07, RN08 e RN10 aplicadas às soluções
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolucaoServiceTest {

    @Mock private SolucaoRepository solucaoRepository;
    @Mock private ProblemaService problemaService;
    @Mock private BuscaService buscaService;
    @Mock private HistoricoService historicoService;
    @Mock private UsuarioLogadoService usuarioLogado;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private SolucaoService service;

    private Usuario colaborador;
    private Usuario gestorDaTi;
    private Usuario gestorDoRh;
    private Problema problema;
    private Solucao solucao;

    @BeforeEach
    void setUp() {
        Departamento ti = Fabrica.departamento(1, "TI");
        Departamento rh = Fabrica.departamento(2, "RH");
        colaborador = Fabrica.usuario(1, Perfil.COLABORADOR, ti);
        gestorDaTi = Fabrica.usuario(2, Perfil.GESTOR, ti);
        gestorDoRh = Fabrica.usuario(3, Perfil.GESTOR, rh);
        problema = Fabrica.problema(10, colaborador, ti, Fabrica.categoria(1, "Redes", null));
        solucao = Fabrica.solucao(100, problema, colaborador);

        when(usuarioLogado.obter(1L)).thenReturn(colaborador);
        when(usuarioLogado.obter(2L)).thenReturn(gestorDaTi);
        when(usuarioLogado.obter(3L)).thenReturn(gestorDoRh);
        when(solucaoRepository.findById(100L)).thenReturn(Optional.of(solucao));
        when(problemaService.buscarEntidade(10L)).thenReturn(problema);
        when(solucaoRepository.save(any(Solucao.class))).thenAnswer(inv -> {
            Solucao s = inv.getArgument(0);
            s.setId(101L);
            return s;
        });
    }

    @Test
    void rn08ValidarSolucaoMarcaOProblemaComoResolvido() {
        service.validar(100L, 2L);

        assertTrue(solucao.isValidada());
        assertEquals(StatusProblema.RESOLVIDO, problema.getStatus());
        verify(historicoService).registrar(AcaoHistorico.VALIDACAO, Historico.TABELA_SOLUCAO, 100L, gestorDaTi);
        verify(historicoService).registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_PROBLEMA, 10L, gestorDaTi);
    }

    @Test
    void rn07GestorDeOutroDepartamentoNaoValida() {
        assertThrows(AcessoNegadoException.class, () -> service.validar(100L, 3L));
        assertEquals(StatusProblema.ABERTO, problema.getStatus());
    }

    @Test
    void rn07ColaboradorNaoValida() {
        assertThrows(AcessoNegadoException.class, () -> service.validar(100L, 1L));
    }

    @Test
    void naoValidaDuasVezes() {
        solucao.setValidada(true);
        assertThrows(RegraNegocioException.class, () -> service.validar(100L, 2L));
    }

    @Test
    void rn10RegistrarSolucaoGeraHistoricoEReindexa() {
        service.registrar(10L, new DescricaoFormDTO("Reiniciar o spooler de impressão"), 1L);

        verify(historicoService).registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_SOLUCAO, 101L, colaborador);
        verify(buscaService).indexar(problema);
    }

    @Test
    void rn05NaoRegistraSolucaoEmProblemaArquivado() {
        problema.setAtivo(false);
        assertThrows(RegraNegocioException.class,
                () -> service.registrar(10L, new DescricaoFormDTO("Qualquer"), 1L));
        verify(solucaoRepository, never()).save(any());
    }

    @Test
    void rn09ArquivarSolucaoNaoExcluiDoBanco() {
        service.arquivar(100L, 1L);

        assertEquals(false, solucao.isAtivo());
        verify(solucaoRepository, never()).delete(any());
        verify(historicoService).registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_SOLUCAO, 100L, colaborador);
    }
}
