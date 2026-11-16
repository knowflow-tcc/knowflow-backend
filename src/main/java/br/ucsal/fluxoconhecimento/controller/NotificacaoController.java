package br.ucsal.fluxoconhecimento.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.NotificacoesDTO;
import br.ucsal.fluxoconhecimento.service.NotificacaoService;

// RF21 - notificações do usuário logado (sino no topo das telas)
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public NotificacoesDTO listar(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return notificacaoService.listar(eu.getId());
    }

    @PatchMapping("/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLida(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        notificacaoService.marcarComoLida(id, eu.getId());
    }

    @PatchMapping("/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasComoLidas(@AuthenticationPrincipal UsuarioAutenticado eu) {
        notificacaoService.marcarTodasComoLidas(eu.getId());
    }
}
