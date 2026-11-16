package br.ucsal.fluxoconhecimento.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ucsal.fluxoconhecimento.config.UsuarioAutenticado;
import br.ucsal.fluxoconhecimento.service.RelatorioService;

// RF22 - relatório de problemas em PDF
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/problemas.pdf")
    public ResponseEntity<byte[]> problemas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long departamentoId,
            @AuthenticationPrincipal UsuarioAutenticado eu) {
        byte[] pdf = relatorioService.problemasPdf(de, ate, departamentoId, eu.getId());
        String nome = "relatorio-problemas-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(pdf);
    }
}
