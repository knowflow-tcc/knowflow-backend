package br.ucsal.fluxoconhecimento.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.TokenRecuperacaoSenha;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.TokenRecuperacaoSenhaRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// RF14 - recuperação de senha por e-mail
@Service
public class RecuperacaoSenhaService {

    static final int VALIDADE_MINUTOS = 30;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEmailService envioEmail;
    private final HistoricoService historicoService;
    private final String urlBase;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository, TokenRecuperacaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder, EnvioEmailService envioEmail,
                                   HistoricoService historicoService, @Value("${knowflow.url-base}") String urlBase) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioEmail = envioEmail;
        this.historicoService = historicoService;
        this.urlBase = urlBase;
    }

    // Passo 1 - gera o link e envia por e-mail
    @Transactional
    public void solicitar(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraNegocioException("email", "Informe o seu e-mail.");
        }
        usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(Usuario::isAtivo)
                .ifPresent(this::enviarLink);
    }

    // Passo 2 - troca a senha usando o código do link
    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRecuperacaoSenha registro = tokenRepository.findByTokenHash(hash(token == null ? "" : token))
                .filter(t -> t.valido(LocalDateTime.now()))
                .orElseThrow(() -> new RegraNegocioException(
                        "Este link de recuperação é inválido, já foi usado ou expirou. Peça um novo link."));
        Usuario usuario = registro.getUsuario();
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("Seu usuário está desativado. Procure o administrador.");
        }
        PoliticaSenha.validar(novaSenha);

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        registro.setUsado(true);
        tokenRepository.invalidarPendentes(usuario.getId());
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_USUARIO, usuario.getId(), usuario);
    }

    private void enviarLink(Usuario usuario) {
        tokenRepository.invalidarPendentes(usuario.getId());
        String token = novoToken();
        tokenRepository.save(new TokenRecuperacaoSenha(hash(token), usuario,
                LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS)));

        String link = urlBase + "/recuperar-senha.html?token=" + token;
        envioEmail.enviar(usuario.getEmail(), "KnowFlow - recuperação de senha",
                "Olá, " + usuario.getNome() + ".\n\n"
                        + "Recebemos um pedido para redefinir a sua senha no KnowFlow.\n"
                        + "Para criar uma nova senha, acesse o link abaixo (válido por " + VALIDADE_MINUTOS + " minutos):\n\n"
                        + link + "\n\n"
                        + "Se você não fez este pedido, ignore este e-mail: a sua senha continua a mesma.");
    }

    private static String novoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
