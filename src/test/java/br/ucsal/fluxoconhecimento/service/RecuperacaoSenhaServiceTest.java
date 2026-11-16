package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.ucsal.fluxoconhecimento.Fabrica;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.TokenRecuperacaoSenha;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.TokenRecuperacaoSenhaRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// RF14 - recuperação de senha por e-mail (com RNF01 e RNF09)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecuperacaoSenhaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacaoSenhaRepository tokenRepository;
    @Mock private EnvioEmailService envioEmail;
    @Mock private HistoricoService historicoService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private RecuperacaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new RecuperacaoSenhaService(usuarioRepository, tokenRepository, passwordEncoder, envioEmail,
                historicoService, "http://localhost:8080");
        usuario = Fabrica.usuario(1, Perfil.COLABORADOR, Fabrica.departamento(1, "TI"));
        usuario.setSenhaHash(passwordEncoder.encode("senhaAntiga1"));
        when(usuarioRepository.findByEmailIgnoreCase("usuario1@empresa.com")).thenReturn(Optional.of(usuario));
    }

    // Pede o link e devolve o código que foi enviado no e-mail
    private String pedirLink() {
        service.solicitar("usuario1@empresa.com");
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviar(eq("usuario1@empresa.com"), anyString(), texto.capture());
        String corpo = texto.getValue();
        int inicio = corpo.indexOf("token=") + "token=".length();
        return corpo.substring(inicio, corpo.indexOf('\n', inicio));
    }

    @Test
    void enviaUmLinkEGuardaApenasOHashDoCodigo() {
        String token = pedirLink();

        ArgumentCaptor<TokenRecuperacaoSenha> salvo = ArgumentCaptor.forClass(TokenRecuperacaoSenha.class);
        verify(tokenRepository).save(salvo.capture());
        assertNotEquals(token, salvo.getValue().getTokenHash());
        assertEquals(RecuperacaoSenhaService.hash(token), salvo.getValue().getTokenHash());
        assertTrue(salvo.getValue().getExpiraEm().isAfter(LocalDateTime.now().plusMinutes(29)));
    }

    @Test
    void emailInexistenteOuUsuarioDesativadoNaoRecebeNada() {
        service.solicitar("nao-existe@empresa.com");
        usuario.setAtivo(false);
        service.solicitar("usuario1@empresa.com");

        verify(envioEmail, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void redefinirTrocaASenhaEInvalidaOLink() {
        TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha(RecuperacaoSenhaService.hash("abc"), usuario,
                LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByTokenHash(RecuperacaoSenhaService.hash("abc"))).thenReturn(Optional.of(registro));

        service.redefinir("abc", "novaSenha2026");

        assertTrue(passwordEncoder.matches("novaSenha2026", usuario.getSenhaHash()));
        assertTrue(registro.isUsado());
        verify(historicoService).registrar(any(), eq("usuario"), eq(1L), eq(usuario));
    }

    @Test
    void linkExpiradoOuJaUsadoERecusado() {
        TokenRecuperacaoSenha expirado = new TokenRecuperacaoSenha("h1", usuario, LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(RecuperacaoSenhaService.hash("velho"))).thenReturn(Optional.of(expirado));
        TokenRecuperacaoSenha usado = new TokenRecuperacaoSenha("h2", usuario, LocalDateTime.now().plusMinutes(10));
        usado.setUsado(true);
        when(tokenRepository.findByTokenHash(RecuperacaoSenhaService.hash("usado"))).thenReturn(Optional.of(usado));

        assertThrows(RegraNegocioException.class, () -> service.redefinir("velho", "novaSenha2026"));
        assertThrows(RegraNegocioException.class, () -> service.redefinir("usado", "novaSenha2026"));
        assertTrue(passwordEncoder.matches("senhaAntiga1", usuario.getSenhaHash()));
    }

    @Test
    void rnf09NovaSenhaFracaERecusada() {
        TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha("h", usuario, LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByTokenHash(RecuperacaoSenhaService.hash("abc"))).thenReturn(Optional.of(registro));

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.redefinir("abc", "123"));
        assertEquals("senha", erro.getCampo());
        assertTrue(!registro.isUsado());
    }
}
