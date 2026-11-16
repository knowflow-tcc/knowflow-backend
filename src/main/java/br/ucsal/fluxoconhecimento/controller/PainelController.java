package br.ucsal.fluxoconhecimento.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.PainelDTO;
import br.ucsal.fluxoconhecimento.service.PainelService;

// RF13 - painel de indicadores
@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painelService;

    public PainelController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping
    public PainelDTO painel(@RequestParam(required = false) Long departamentoId,
                            @AuthenticationPrincipal UsuarioAutenticado eu) {
        return painelService.gerar(departamentoId, eu.getId());
    }
}
