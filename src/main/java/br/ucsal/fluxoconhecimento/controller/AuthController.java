package br.ucsal.fluxoconhecimento.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.LoginDTO;
import br.ucsal.fluxoconhecimento.dto.RecuperarSenhaDTO;
import br.ucsal.fluxoconhecimento.dto.RedefinirSenhaDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioLogadoDTO;
import br.ucsal.fluxoconhecimento.excecao.FalhaAutenticacaoException;
import br.ucsal.fluxoconhecimento.service.RecuperacaoSenhaService;
import br.ucsal.fluxoconhecimento.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

// RF01 - autenticação por e-mail e senha
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UsuarioService usuarioService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository, UsuarioService usuarioService,
                          RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.usuarioService = usuarioService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "cabecalho", token.getHeaderName());
    }

    @PostMapping("/login")
    public UsuarioLogadoDTO login(@Valid @RequestBody LoginDTO dados, HttpServletRequest request,
                                  HttpServletResponse response) {
        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(dados.email().trim(), dados.senha()));
        } catch (DisabledException e) {
            throw new FalhaAutenticacaoException("Seu usuário está desativado. Procure o administrador.");
        } catch (AuthenticationException e) {
            throw new FalhaAutenticacaoException("E-mail ou senha incorretos. Confira os dados e tente novamente.");
        }

        // Nova sessão a cada login (proteção contra fixação de sessão) e guarda o usuário nela
        request.getSession(true);
        request.changeSessionId();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        return usuarioService.usuarioLogado(usuario.getId());
    }

    @GetMapping("/eu")
    public UsuarioLogadoDTO eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.usuarioLogado(usuario.getId());
    }

    // Sempre 204, exista o e-mail ou não (não revela quem tem cadastro)
    @PostMapping("/recuperar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarSenha(@Valid @RequestBody RecuperarSenhaDTO dados) {
        recuperacaoSenhaService.solicitar(dados.email());
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaDTO dados) {
        recuperacaoSenhaService.redefinir(dados.token(), dados.senha());
    }
}
