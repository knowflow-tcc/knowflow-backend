package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.context.ApplicationEventPublisher;

import br.ucsal.fluxoconhecimento.Fabrica;
import br.ucsal.fluxoconhecimento.dto.CategoriaDTO;
import br.ucsal.fluxoconhecimento.dto.CategoriaFormDTO;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.CategoriaRepository;

// RF07 - categorias em árvore, geridas pelo gestor
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HistoricoService historicoService;
    @Mock private UsuarioLogadoService usuarioLogado;
    @Mock private ApplicationEventPublisher eventos;
    @InjectMocks private CategoriaService service;

    private final List<Categoria> banco = new ArrayList<>();
    private Categoria infraestrutura;
    private Categoria redes;

    @BeforeEach
    void setUp() {
        Departamento ti = Fabrica.departamento(1, "TI");
        Usuario gestor = Fabrica.usuario(1, Perfil.GESTOR, ti);
        Usuario colaborador = Fabrica.usuario(2, Perfil.COLABORADOR, ti);
        infraestrutura = Fabrica.categoria(1, "Infraestrutura", null);
        redes = Fabrica.categoria(2, "Redes", infraestrutura);
        banco.add(infraestrutura);
        banco.add(redes);

        when(usuarioLogado.obter(1L)).thenReturn(gestor);
        when(usuarioLogado.obter(2L)).thenReturn(colaborador);
        when(categoriaRepository.findAll()).thenAnswer(inv -> List.copyOf(banco));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(infraestrutura));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(redes));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(3L);
            banco.add(c);
            return c;
        });
    }

    @Test
    void cadastraSubcategoriaComCaminhoCompleto() {
        CategoriaDTO dto = service.cadastrar(new CategoriaFormDTO("VPN", 2L), 1L);

        assertEquals("Infraestrutura > Redes > VPN", dto.caminho());
        assertEquals(2, dto.nivel());
    }

    @Test
    void impedeCicloNaHierarquia() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.atualizar(1L, new CategoriaFormDTO("Infraestrutura", 2L), 1L));
        assertEquals("idPai", erro.getCampo());
    }

    @Test
    void impedeNomeRepetidoNoMesmoNivel() {
        assertThrows(RegraNegocioException.class, () -> service.cadastrar(new CategoriaFormDTO("redes", 1L), 1L));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void somenteGestorGerenciaCategorias() {
        assertThrows(AcessoNegadoException.class, () -> service.cadastrar(new CategoriaFormDTO("Nova", null), 2L));
    }

    @Test
    void moverCategoriaAvisaABuscaParaReindexar() {
        service.atualizar(2L, new CategoriaFormDTO("Redes", null), 1L);

        verify(eventos).publishEvent(any(CategoriasAlteradasEvent.class));
    }
}
