package br.ucsal.fluxoconhecimento.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.CausaDTO;
import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.service.CausaService;
import jakarta.validation.Valid;

// RF15 - editar e arquivar causas
@RestController
@RequestMapping("/api/causas")
public class CausaController {

    private final CausaService causaService;

    public CausaController(CausaService causaService) {
        this.causaService = causaService;
    }

    @PutMapping("/{id}")
    public CausaDTO atualizar(@PathVariable Long id, @Valid @RequestBody DescricaoFormDTO form,
                              @AuthenticationPrincipal UsuarioAutenticado eu) {
        return causaService.atualizar(id, form, eu.getId());
    }

    @PatchMapping("/{id}/arquivar")
    public CausaDTO arquivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return causaService.arquivar(id, eu.getId());
    }
}
