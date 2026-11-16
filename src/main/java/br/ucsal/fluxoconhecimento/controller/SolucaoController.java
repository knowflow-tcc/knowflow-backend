package br.ucsal.fluxoconhecimento.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.AnexoDTO;
import br.ucsal.fluxoconhecimento.dto.AvaliacaoFormDTO;
import br.ucsal.fluxoconhecimento.dto.ComentarioDTO;
import br.ucsal.fluxoconhecimento.dto.ComentarioFormDTO;
import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.service.AnexoService;
import br.ucsal.fluxoconhecimento.service.AvaliacaoSolucaoService;
import br.ucsal.fluxoconhecimento.service.ComentarioService;
import br.ucsal.fluxoconhecimento.service.SolucaoService;
import jakarta.validation.Valid;

// Operações sobre uma solução
@RestController
@RequestMapping("/api/solucoes")
public class SolucaoController {

    private final SolucaoService solucaoService;
    private final ComentarioService comentarioService;
    private final AvaliacaoSolucaoService avaliacaoService;
    private final AnexoService anexoService;

    public SolucaoController(SolucaoService solucaoService, ComentarioService comentarioService,
                             AvaliacaoSolucaoService avaliacaoService, AnexoService anexoService) {
        this.solucaoService = solucaoService;
        this.comentarioService = comentarioService;
        this.avaliacaoService = avaliacaoService;
        this.anexoService = anexoService;
    }

    @PutMapping("/{id}")
    public SolucaoDTO atualizar(@PathVariable Long id, @Valid @RequestBody DescricaoFormDTO form,
                                @AuthenticationPrincipal UsuarioAutenticado eu) {
        return solucaoService.atualizar(id, form, eu.getId());
    }

    @PatchMapping("/{id}/arquivar")
    public SolucaoDTO arquivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return solucaoService.arquivar(id, eu.getId());
    }

    @PatchMapping("/{id}/validar")
    public SolucaoDTO validar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return solucaoService.validar(id, eu.getId());
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioDTO comentar(@PathVariable Long id, @Valid @RequestBody ComentarioFormDTO form,
                                  @AuthenticationPrincipal UsuarioAutenticado eu) {
        return comentarioService.comentar(id, form, eu.getId());
    }

    @PutMapping("/{id}/avaliacao")
    public SolucaoDTO avaliar(@PathVariable Long id, @Valid @RequestBody AvaliacaoFormDTO form,
                              @AuthenticationPrincipal UsuarioAutenticado eu) {
        return avaliacaoService.avaliar(id, form.ajudou(), eu.getId());
    }

    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoDTO anexar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
                           @AuthenticationPrincipal UsuarioAutenticado eu) {
        return anexoService.anexarASolucao(id, arquivo, eu.getId());
    }
}
