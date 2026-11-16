package br.ucsal.fluxoconhecimento.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.dto.UsuarioDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioFormDTO;
import br.ucsal.fluxoconhecimento.service.UsuarioService;
import jakarta.validation.Valid;

// RF02/RF12 - gestão de usuários (somente administrador)
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioDTO> listar(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.listar(eu.getId());
    }

    @GetMapping("/{id}")
    public UsuarioDTO buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.buscar(id, eu.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO cadastrar(@Valid @RequestBody UsuarioFormDTO form, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.cadastrar(form, eu.getId());
    }

    @PutMapping("/{id}")
    public UsuarioDTO atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioFormDTO form,
                                @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.atualizar(id, form, eu.getId());
    }

    @PatchMapping("/{id}/desativar")
    public UsuarioDTO desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.desativar(id, eu.getId());
    }

    @PatchMapping("/{id}/reativar")
    public UsuarioDTO reativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.reativar(id, eu.getId());
    }

    @PatchMapping("/{id}/anonimizar")
    public UsuarioDTO anonimizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return usuarioService.anonimizar(id, eu.getId());
    }
}
