package br.ucsal.fluxoconhecimento.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;

// RF22 - relatório de problemas em PDF (OpenPDF)
@Service
public class RelatorioService {

    static final int PERIODO_MAXIMO_DIAS = 366 * 5;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color COR_CABECALHO = new Color(37, 99, 235);
    private static final Color COR_LINHA_PAR = new Color(241, 245, 249);

    private final ProblemaRepository problemaRepository;
    private final DepartamentoService departamentoService;
    private final CategoriaService categoriaService;
    private final UsuarioLogadoService usuarioLogado;

    public RelatorioService(ProblemaRepository problemaRepository, DepartamentoService departamentoService,
                            CategoriaService categoriaService, UsuarioLogadoService usuarioLogado) {
        this.problemaRepository = problemaRepository;
        this.departamentoService = departamentoService;
        this.categoriaService = categoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    // Período informado (inclusive); sem datas = mês atual
    @Transactional(readOnly = true)
    public byte[] problemasPdf(LocalDate de, LocalDate ate, Long idDepartamentoFiltro, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(usuario, Perfil.GESTOR, Perfil.ADMINISTRADOR);

        LocalDate inicio = de != null ? de : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = ate != null ? ate : LocalDate.now();
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("ate", "A data final não pode ser anterior à data inicial.");
        }
        if (inicio.plusDays(PERIODO_MAXIMO_DIAS).isBefore(fim)) {
            throw new RegraNegocioException("ate", "Escolha um período de no máximo 5 anos.");
        }

        Departamento departamento = usuario.getPerfil() == Perfil.GESTOR
                ? usuario.getDepartamento()
                : (idDepartamentoFiltro == null ? null : departamentoService.buscarEntidade(idDepartamentoFiltro));
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimDt = fim.plusDays(1).atStartOfDay();
        List<Problema> problemas = departamento == null
                ? problemaRepository.doPeriodo(inicioDt, fimDt)
                : problemaRepository.doPeriodoEDepartamento(inicioDt, fimDt, departamento.getId());

        return gerarPdf(problemas, inicio, fim, departamento, usuario);
    }

    private byte[] gerarPdf(List<Problema> problemas, LocalDate inicio, LocalDate fim,
                            Departamento departamento, Usuario usuario) {
        ArvoreCategorias arvore = categoriaService.arvore();
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        Font cabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(documento, saida);
        documento.open();

        documento.add(new Paragraph("KnowFlow - Relatório de problemas", titulo));
        documento.add(new Paragraph("Período: " + DATA.format(inicio) + " a " + DATA.format(fim)
                + "   |   Departamento: " + (departamento == null ? "Todos" : departamento.getNome()), normal));
        documento.add(new Paragraph("Gerado por " + usuario.getNome() + " em " + DATA_HORA.format(LocalDateTime.now())
                + ". Problemas arquivados não entram no relatório.", pequena));

        // Resumo
        long abertos = problemas.stream().filter(p -> p.getStatus() == StatusProblema.ABERTO).count();
        long resolvidos = problemas.size() - abertos;
        List<Solucao> solucoes = problemas.stream().flatMap(p -> p.getSolucoesAtivas().stream()).toList();
        long validadas = solucoes.stream().filter(Solucao::isValidada).count();
        int taxa = problemas.isEmpty() ? 0 : (int) Math.round(100.0 * resolvidos / problemas.size());

        documento.add(espaco(subtitulo, "Resumo"));
        PdfPTable resumo = new PdfPTable(6);
        resumo.setWidthPercentage(100);
        for (String rotulo : List.of("Problemas", "Abertos", "Resolvidos", "Taxa de resolução", "Soluções", "Soluções validadas")) {
            resumo.addCell(celulaCabecalho(rotulo, cabecalho));
        }
        for (String valor : List.of(String.valueOf(problemas.size()), String.valueOf(abertos), String.valueOf(resolvidos),
                taxa + "%", String.valueOf(solucoes.size()), String.valueOf(validadas))) {
            resumo.addCell(celula(valor, normal, null, Element.ALIGN_CENTER));
        }
        documento.add(resumo);

        // Problemas por categoria principal (raiz da árvore de categorias)
        Map<String, Long> porCategoria = new HashMap<>();
        for (Problema p : problemas) {
            List<String> caminho = arvore.caminho(p.getCategoria().getId());
            porCategoria.merge(caminho.isEmpty() ? p.getCategoria().getNome() : caminho.get(0), 1L, Long::sum);
        }
        if (!porCategoria.isEmpty()) {
            documento.add(espaco(subtitulo, "Problemas por categoria"));
            PdfPTable categorias = new PdfPTable(new float[] {4, 1});
            categorias.setWidthPercentage(50);
            categorias.setHorizontalAlignment(Element.ALIGN_LEFT);
            categorias.addCell(celulaCabecalho("Categoria", cabecalho));
            categorias.addCell(celulaCabecalho("Problemas", cabecalho));
            porCategoria.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .forEach(e -> {
                        categorias.addCell(celula(e.getKey(), normal, null, Element.ALIGN_LEFT));
                        categorias.addCell(celula(String.valueOf(e.getValue()), normal, null, Element.ALIGN_CENTER));
                    });
            documento.add(categorias);
        }

        // Lista detalhada
        documento.add(espaco(subtitulo, "Problemas registrados no período"));
        if (problemas.isEmpty()) {
            documento.add(new Paragraph("Nenhum problema registrado neste período.", normal));
        } else {
            PdfPTable tabela = new PdfPTable(new float[] {0.6f, 1.1f, 4f, 2.6f, 1.6f, 1.6f, 1f, 1f});
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            for (String rotulo : List.of("Nº", "Data", "Título", "Categoria", "Departamento", "Autor", "Situação", "Soluções")) {
                tabela.addCell(celulaCabecalho(rotulo, cabecalho));
            }
            int linha = 0;
            for (Problema p : problemas) {
                Color fundo = linha++ % 2 == 1 ? COR_LINHA_PAR : null;
                long totalSolucoes = p.getSolucoesAtivas().size();
                boolean temValidada = p.getSolucoesAtivas().stream().anyMatch(Solucao::isValidada);
                tabela.addCell(celula(String.valueOf(p.getId()), normal, fundo, Element.ALIGN_CENTER));
                tabela.addCell(celula(DATA.format(p.getDataRegistro()), normal, fundo, Element.ALIGN_CENTER));
                tabela.addCell(celula(p.getTitulo(), normal, fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(arvore.caminhoTexto(p.getCategoria().getId()), normal, fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(p.getDepartamento().getNome(), normal, fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(p.getUsuario().getNome(), normal, fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(p.getStatus().getRotulo(), normal, fundo, Element.ALIGN_CENTER));
                tabela.addCell(celula(totalSolucoes + (temValidada ? " (validada)" : ""), normal, fundo, Element.ALIGN_CENTER));
            }
            documento.add(tabela);
        }

        documento.close();
        return saida.toByteArray();
    }

    private static Paragraph espaco(Font fonte, String texto) {
        Paragraph paragrafo = new Paragraph(texto, fonte);
        paragrafo.setSpacingBefore(14);
        paragrafo.setSpacingAfter(6);
        return paragrafo;
    }

    private static PdfPCell celulaCabecalho(String texto, Font fonte) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte));
        celula.setBackgroundColor(COR_CABECALHO);
        celula.setHorizontalAlignment(Element.ALIGN_CENTER);
        celula.setPadding(5);
        return celula;
    }

    private static PdfPCell celula(String texto, Font fonte, Color fundo, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto == null ? "" : texto, fonte));
        if (fundo != null) {
            celula.setBackgroundColor(fundo);
        }
        celula.setHorizontalAlignment(alinhamento);
        celula.setPadding(4);
        return celula;
    }
}
