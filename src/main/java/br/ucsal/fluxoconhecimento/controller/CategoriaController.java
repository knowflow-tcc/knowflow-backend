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
import br.ucsal.fluxoconhecimento.dto.CategoriaDTO;
import br.ucsal.fluxoconhecimento.dto.CategoriaFormDTO;
import br.ucsal.fluxoconhecimento.dto.CategoriaNoDTO;
import br.ucsal.fluxoconhecimento.service.CategoriaService;
import jakarta.validation.Valid;

// RF07 - categorias em árvore
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Lista na ordem da árvore (pré-ordem), com nível e caminho
    @GetMapping
    public List<CategoriaDTO> listar(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return categoriaService.listar(eu.getId());
    }

    // A árvore aninhada: raízes com seus filhos
    @GetMapping("/arvore")
    public List<CategoriaNoDTO> arvore(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return categoriaService.listarArvore(eu.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDTO cadastrar(@Valid @RequestBody CategoriaFormDTO form, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return categoriaService.cadastrar(form, eu.getId());
    }

    @PutMapping("/{id}")
    public CategoriaDTO atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaFormDTO form,
                                  @AuthenticationPrincipal UsuarioAutenticado eu) {
        return categoriaService.atualizar(id, form, eu.getId());
    }
}
