package br.ucsal.fluxoconhecimento.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import br.ucsal.fluxoconhecimento.dto.CausaDTO;
import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.dto.ProblemaDetalheDTO;
import br.ucsal.fluxoconhecimento.dto.ProblemaFormDTO;
import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.dto.StatusFormDTO;
import br.ucsal.fluxoconhecimento.service.AnexoService;
import br.ucsal.fluxoconhecimento.service.CausaService;
import br.ucsal.fluxoconhecimento.service.FavoritoService;
import br.ucsal.fluxoconhecimento.service.ProblemaService;
import br.ucsal.fluxoconhecimento.service.SolucaoService;
import jakarta.validation.Valid;

// Problemas e a inclusão de causas e soluções neles
@RestController
@RequestMapping("/api/problemas")
public class ProblemaController {

    private final ProblemaService problemaService;
    private final CausaService causaService;
    private final SolucaoService solucaoService;
    private final AnexoService anexoService;
    private final FavoritoService favoritoService;

    public ProblemaController(ProblemaService problemaService, CausaService causaService, SolucaoService solucaoService,
                              AnexoService anexoService, FavoritoService favoritoService) {
        this.problemaService = problemaService;
        this.causaService = causaService;
        this.solucaoService = solucaoService;
        this.anexoService = anexoService;
        this.favoritoService = favoritoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemaDetalheDTO registrar(@Valid @RequestBody ProblemaFormDTO form,
                                        @AuthenticationPrincipal UsuarioAutenticado eu) {
        return problemaService.registrar(form, eu.getId());
    }

    @GetMapping("/{id}")
    public ProblemaDetalheDTO detalhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return problemaService.detalhar(id, eu.getId());
    }

    @PutMapping("/{id}")
    public ProblemaDetalheDTO atualizar(@PathVariable Long id, @Valid @RequestBody ProblemaFormDTO form,
                                        @AuthenticationPrincipal UsuarioAutenticado eu) {
        return problemaService.atualizar(id, form, eu.getId());
    }

    @PatchMapping("/{id}/arquivar")
    public ProblemaDetalheDTO arquivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return problemaService.arquivar(id, eu.getId());
    }

    @PatchMapping("/{id}/status")
    public ProblemaDetalheDTO alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusFormDTO form,
                                            @AuthenticationPrincipal UsuarioAutenticado eu) {
        return problemaService.alterarStatus(id, form.status(), eu.getId());
    }

    @PostMapping("/{id}/causas")
    @ResponseStatus(HttpStatus.CREATED)
    public CausaDTO registrarCausa(@PathVariable Long id, @Valid @RequestBody DescricaoFormDTO form,
                                   @AuthenticationPrincipal UsuarioAutenticado eu) {
        return causaService.registrar(id, form, eu.getId());
    }

    @PostMapping("/{id}/solucoes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolucaoDTO registrarSolucao(@PathVariable Long id, @Valid @RequestBody DescricaoFormDTO form,
                                       @AuthenticationPrincipal UsuarioAutenticado eu) {
        return solucaoService.registrar(id, form, eu.getId());
    }

    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoDTO anexar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
                           @AuthenticationPrincipal UsuarioAutenticado eu) {
        return anexoService.anexarAoProblema(id, arquivo, eu.getId());
    }

    @PutMapping("/{id}/favorito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarFavorito(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        favoritoService.marcar(id, eu.getId());
    }

    @DeleteMapping("/{id}/favorito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcarFavorito(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        favoritoService.desmarcar(id, eu.getId());
    }
}
