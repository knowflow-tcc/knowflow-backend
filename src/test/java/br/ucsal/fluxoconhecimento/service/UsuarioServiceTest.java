package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import br.ucsal.fluxoconhecimento.Fabrica;
import br.ucsal.fluxoconhecimento.dto.UsuarioDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioFormDTO;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// RF02 com RN02, RN03, RNF01, RNF09 e RNF02 (LGPD)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DepartamentoRepository departamentoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HistoricoService historicoService;
    @Mock private UsuarioLogadoService usuarioLogado;
    @InjectMocks private UsuarioService service;

    private Departamento ti;
    private Usuario admin;
    private Usuario colaborador;

    @BeforeEach
    void setUp() {
        ti = Fabrica.departamento(1, "TI");
        admin = Fabrica.usuario(1, Perfil.ADMINISTRADOR, ti);
        colaborador = Fabrica.usuario(2, Perfil.COLABORADOR, ti);

        when(usuarioLogado.obter(1L)).thenReturn(admin);
        when(usuarioLogado.obter(2L)).thenReturn(colaborador);
        when(departamentoRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(colaborador));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "bcrypt:" + inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });
    }

    private UsuarioFormDTO form(String email, String senha) {
        return new UsuarioFormDTO("Maria Souza", email, Perfil.COLABORADOR, 1L, senha);
    }

    @Test
    void cadastraComEmailMinusculoESenhaComHash() {
        UsuarioDTO dto = service.cadastrar(form("  Maria@Empresa.com ", "suporte2026"), 1L);

        assertEquals("maria@empresa.com", dto.email());
        assertEquals("TI", dto.departamentoNome());
        verify(passwordEncoder).encode("suporte2026");
    }

    @Test
    void rn02EmailRepetidoERecusado() {
        when(usuarioRepository.existsByEmailIgnoreCase("maria@empresa.com")).thenReturn(true);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar(form("maria@empresa.com", "suporte2026"), 1L));
        assertEquals("email", erro.getCampo());
    }

    @Test
    void rnf09SenhaFracaERecusada() {
        assertThrows(RegraNegocioException.class, () -> service.cadastrar(form("maria@empresa.com", "123"), 1L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void somenteAdministradorGerenciaUsuarios() {
        assertThrows(AcessoNegadoException.class,
                () -> service.cadastrar(form("maria@empresa.com", "suporte2026"), 2L));
    }

    @Test
    void naoDesativaOProprioUsuario() {
        assertThrows(RegraNegocioException.class, () -> service.desativar(1L, 1L));
    }

    @Test
    void naoRebaixaOUnicoAdministradorAtivo() {
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)).thenReturn(1L);
        UsuarioFormDTO rebaixar = new UsuarioFormDTO("Admin", "usuario1@empresa.com", Perfil.GESTOR, 1L, null);

        assertThrows(RegraNegocioException.class, () -> service.atualizar(1L, rebaixar, 1L));
    }

    @Test
    void desativarNaoExcluiOUsuario() {
        service.desativar(2L, 1L);

        assertFalse(colaborador.isAtivo());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void lgpdAnonimizarApagaNomeEEmailDeUsuarioDesativado() {
        colaborador.setAtivo(false);

        service.anonimizar(2L, 1L);

        assertEquals("Usuário anonimizado #2", colaborador.getNome());
        assertEquals("anonimizado-2@knowflow.invalid", colaborador.getEmail());
        assertNotEquals("hash", colaborador.getSenhaHash());
    }

    @Test
    void lgpdNaoAnonimizaUsuarioAtivo() {
        assertThrows(RegraNegocioException.class, () -> service.anonimizar(2L, 1L));
    }
}
