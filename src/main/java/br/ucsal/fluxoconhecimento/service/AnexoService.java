package br.ucsal.fluxoconhecimento.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.ucsal.fluxoconhecimento.dto.AnexoDTO;
import br.ucsal.fluxoconhecimento.dto.AnexoInfo;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Anexo;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.AnexoRepository;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;

// RF16 - anexar arquivos (imagens e documentos) a problemas e soluções
@Service
public class AnexoService {

    // Extensões aceitas e o tipo (MIME) com que o arquivo é devolvido no download
    static final Map<String, String> TIPOS_PERMITIDOS = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("log", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("zip", "application/zip"));

    private final AnexoRepository anexoRepository;
    private final SolucaoRepository solucaoRepository;
    private final ProblemaService problemaService;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;

    public AnexoService(AnexoRepository anexoRepository, SolucaoRepository solucaoRepository,
                        ProblemaService problemaService, HistoricoService historicoService,
                        UsuarioLogadoService usuarioLogado) {
        this.anexoRepository = anexoRepository;
        this.solucaoRepository = solucaoRepository;
        this.problemaService = problemaService;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public AnexoDTO anexarAoProblema(Long idProblema, MultipartFile arquivo, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Problema problema = problemaService.buscarAtivo(idProblema);
        Permissoes.exigir(Permissoes.podeAnexarAoProblema(usuario, problema),
                Permissoes.motivoSemPermissaoDeEdicao(usuario));

        Anexo anexo = novoAnexo(arquivo, usuario);
        anexo.setProblema(problema);
        return salvar(anexo, usuario, problema);
    }

    @Transactional
    public AnexoDTO anexarASolucao(Long idSolucao, MultipartFile arquivo, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Solucao solucao = solucaoRepository.findById(idSolucao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solução não encontrada."));
        if (!solucao.isAtivo() || !solucao.getProblema().isAtivo()) {
            throw new RegraNegocioException("Esta solução (ou o seu problema) está arquivada e não recebe anexos.");
        }
        Permissoes.exigir(Permissoes.podeAnexarASolucao(usuario, solucao),
                Permissoes.motivoSemPermissaoDeEdicao(usuario));

        Anexo anexo = novoAnexo(arquivo, usuario);
        anexo.setSolucao(solucao);
        return salvar(anexo, usuario, solucao.getProblema());
    }

    // Download: qualquer usuário logado (todos consultam o conhecimento - RF20)
    @Transactional(readOnly = true)
    public Anexo baixar(Long id, Long idUsuarioLogado) {
        usuarioLogado.obter(idUsuarioLogado);
        Anexo anexo = anexoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado."));
        if (!anexo.isAtivo()) {
            throw new RecursoNaoEncontradoException("Este anexo foi arquivado.");
        }
        return anexo;
    }

    @Transactional
    public void arquivar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Anexo anexo = anexoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado."));
        Problema problema = anexo.getProblemaDeOrigem();
        if (!anexo.isAtivo() || !problema.isAtivo()) {
            throw new RegraNegocioException("Este anexo (ou o seu problema) já está arquivado.");
        }
        Permissoes.exigir(Permissoes.podeArquivarAnexo(usuario, anexo.getUsuario().getId(), problema.getDepartamento()),
                Permissoes.motivoSemPermissaoDeEdicao(usuario));

        anexo.setAtivo(false);
        historicoService.registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_ANEXO, anexo.getId(), usuario);
    }

    // Anexos ativos do problema e das suas soluções, sem o conteúdo dos arquivos
    @Transactional(readOnly = true)
    public List<AnexoInfo> listarDoProblema(Long idProblema) {
        return anexoRepository.listarDoProblema(idProblema);
    }

    // Tipo (MIME) usado no download, a partir da extensão do nome do arquivo
    public static String tipoPorExtensao(String nomeArquivo) {
        return TIPOS_PERMITIDOS.get(extensao(nomeArquivo));
    }

    private Anexo novoAnexo(MultipartFile arquivo, Usuario usuario) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("arquivo", "Escolha um arquivo para anexar.");
        }
        if (arquivo.getSize() > Anexo.TAMANHO_MAXIMO) {
            throw new RegraNegocioException("arquivo", "O arquivo pode ter no máximo 5 MB.");
        }
        String nome = nomeSeguro(arquivo.getOriginalFilename());
        String tipo = tipoPorExtensao(nome);
        if (tipo == null) {
            throw new RegraNegocioException("arquivo",
                    "Tipo de arquivo não permitido. Envie imagens (PNG, JPG, GIF, WEBP), PDF, textos, planilhas, "
                            + "documentos do Office/LibreOffice ou ZIP.");
        }
        try {
            return new Anexo(nome, tipo, arquivo.getBytes(), usuario, LocalDateTime.now());
        } catch (IOException e) {
            throw new RegraNegocioException("arquivo", "Não foi possível ler o arquivo enviado. Tente de novo.");
        }
    }

    private AnexoDTO salvar(Anexo anexo, Usuario usuario, Problema problema) {
        anexoRepository.save(anexo);
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_ANEXO, anexo.getId(), usuario);
        AnexoInfo info = new AnexoInfo(anexo.getId(), anexo.getNomeArquivo(), anexo.getTipoConteudo(), anexo.getTamanho(),
                anexo.getDataHora(), usuario.getId(), usuario.getNome(),
                anexo.getProblema() == null ? null : problema.getId(),
                anexo.getSolucao() == null ? null : anexo.getSolucao().getId());
        return Conversor.anexo(info, usuario, problema.getDepartamento(), problema.isAtivo());
    }

    // Mantém só o nome do arquivo (sem pastas) e remove caracteres de controle
    static String nomeSeguro(String original) {
        String nome = original == null ? "" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nome.isEmpty()) {
            nome = "arquivo";
        }
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }

    private static String extensao(String nome) {
        int ponto = nome == null ? -1 : nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }
}
