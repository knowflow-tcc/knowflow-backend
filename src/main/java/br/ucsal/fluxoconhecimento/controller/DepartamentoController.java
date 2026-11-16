package br.ucsal.fluxoconhecimento.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.DepartamentoDTO;
import br.ucsal.fluxoconhecimento.dto.DepartamentoFormDTO;
import br.ucsal.fluxoconhecimento.service.DepartamentoService;
import jakarta.validation.Valid;

// RF03 - departamentos
@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    public DepartamentoController(DepartamentoService departamentoService) {
        this.departamentoService = departamentoService;
    }

    @GetMapping
    public List<DepartamentoDTO> listar(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return departamentoService.listar(eu.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartamentoDTO cadastrar(@Valid @RequestBody DepartamentoFormDTO form,
                                     @AuthenticationPrincipal UsuarioAutenticado eu) {
        return departamentoService.cadastrar(form, eu.getId());
    }

    @PutMapping("/{id}")
    public DepartamentoDTO atualizar(@PathVariable Long id, @Valid @RequestBody DepartamentoFormDTO form,
                                     @AuthenticationPrincipal UsuarioAutenticado eu) {
        return departamentoService.atualizar(id, form, eu.getId());
    }
}
