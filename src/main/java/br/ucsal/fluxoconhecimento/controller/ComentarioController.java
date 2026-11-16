package br.ucsal.fluxoconhecimento.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.ComentarioDTO;
import br.ucsal.fluxoconhecimento.service.ComentarioService;

// RF17 - arquivar comentários
@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PatchMapping("/{id}/arquivar")
    public ComentarioDTO arquivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return comentarioService.arquivar(id, eu.getId());
    }
}
