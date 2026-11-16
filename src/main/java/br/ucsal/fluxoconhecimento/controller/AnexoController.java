package br.ucsal.fluxoconhecimento.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.model.Anexo;
import br.ucsal.fluxoconhecimento.service.AnexoService;

// RF16 - download e arquivamento de anexos
@RestController
@RequestMapping("/api/anexos")
public class AnexoController {

    private final AnexoService anexoService;

    public AnexoController(AnexoService anexoService) {
        this.anexoService = anexoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        Anexo anexo = anexoService.baixar(id, eu.getId());
        boolean imagem = anexo.getTipoConteudo().startsWith("image/");
        ContentDisposition disposicao = (imagem ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(anexo.getNomeArquivo(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getTipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(anexo.getConteudo());
    }

    @PatchMapping("/{id}/arquivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        anexoService.arquivar(id, eu.getId());
    }
}
