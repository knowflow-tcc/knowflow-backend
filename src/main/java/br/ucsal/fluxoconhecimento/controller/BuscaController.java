package br.ucsal.fluxoconhecimento.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.FiltroBuscaDTO;
import br.ucsal.fluxoconhecimento.dto.ResultadoBuscaDTO;
import br.ucsal.fluxoconhecimento.dto.SemelhanteDTO;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.service.BuscaService;

// RF08/RF19 - busca com filtros e ordenação; RF09 - sugestão de semelhantes
@RestController
@RequestMapping("/api/busca")
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GetMapping
    public List<ResultadoBuscaDTO> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long departamentoId,
            @RequestParam(required = false) StatusProblema status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "false") boolean somenteFavoritos,
            @AuthenticationPrincipal UsuarioAutenticado eu) {
        return buscaService.buscar(new FiltroBuscaDTO(texto, categoriaId, departamentoId, status, de, ate, ordem,
                somenteFavoritos), eu.getId());
    }

    @GetMapping("/semelhantes")
    public List<SemelhanteDTO> semelhantes(@RequestParam(required = false) String texto,
                                           @RequestParam(required = false) Long ignorarId,
                                           @AuthenticationPrincipal UsuarioAutenticado eu) {
        return buscaService.semelhantes(texto, ignorarId, eu.getId());
    }
}
