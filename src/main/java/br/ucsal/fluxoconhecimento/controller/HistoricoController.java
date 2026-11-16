package br.ucsal.fluxoconhecimento.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.HistoricoDTO;
import br.ucsal.fluxoconhecimento.service.HistoricoService;

// RF11 - histórico geral de ações (administrador)
@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public List<HistoricoDTO> listar(@RequestParam(required = false) String tabela,
                                     @AuthenticationPrincipal UsuarioAutenticado eu) {
        return historicoService.listarGeral(tabela, eu.getId());
    }
}
