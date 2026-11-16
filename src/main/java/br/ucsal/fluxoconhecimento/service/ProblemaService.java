package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.AnexoDTO;
import br.ucsal.fluxoconhecimento.dto.AnexoInfo;
import br.ucsal.fluxoconhecimento.dto.ProblemaDetalheDTO;
import br.ucsal.fluxoconhecimento.dto.ProblemaFormDTO;
import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.PalavraChave;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.AnexoRepository;
import br.ucsal.fluxoconhecimento.repository.FavoritoRepository;
import br.ucsal.fluxoconhecimento.repository.PalavraChaveRepository;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;

// Registro do conhecimento - problemas
@Service
public class ProblemaService {

    static final int TAMANHO_MAXIMO_PALAVRA_CHAVE = 40;

    private final ProblemaRepository problemaRepository;
    private final PalavraChaveRepository palavraChaveRepository;
    private final DepartamentoService departamentoService;
    private final CategoriaService categoriaService;
    private final BuscaService buscaService;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;
    private final AnexoRepository anexoRepository;
    private final FavoritoRepository favoritoRepository;

    public ProblemaService(ProblemaRepository problemaRepository, PalavraChaveRepository palavraChaveRepository,
                           DepartamentoService departamentoService, CategoriaService categoriaService,
                           BuscaService buscaService, HistoricoService historicoService,
                           UsuarioLogadoService usuarioLogado, AnexoRepository anexoRepository,
                           FavoritoRepository favoritoRepository) {
        this.problemaRepository = problemaRepository;
        this.palavraChaveRepository = palavraChaveRepository;
        this.departamentoService = departamentoService;
        this.categoriaService = categoriaService;
        this.buscaService = buscaService;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
        this.anexoRepository = anexoRepository;
        this.favoritoRepository = favoritoRepository;
    }

    @Transactional
    public ProblemaDetalheDTO registrar(ProblemaFormDTO form, Long idUsuarioLogado) {
        Usuario autor = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigir(Permissoes.podeRegistrar(autor), "Somente colaboradores e gestores registram problemas.");

        Problema problema = new Problema();
        problema.setUsuario(autor);
        problema.setDataRegistro(LocalDateTime.now());
        problema.setStatus(StatusProblema.ABERTO);
        preencher(problema, form);
        problemaRepository.save(problema);

        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_PROBLEMA, problema.getId(), autor);
        buscaService.indexar(problema);
        return paraDetalhe(problema, autor);
    }

    @Transactional
    public ProblemaDetalheDTO atualizar(Long id, ProblemaFormDTO form, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Problema problema = buscarAtivo(id);
        Permissoes.exigir(Permissoes.podeEditarProblema(usuario, problema), Permissoes.motivoSemPermissaoDeEdicao(usuario));

        preencher(problema, form);
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_PROBLEMA, problema.getId(), usuario);
        buscaService.indexar(problema);
        return paraDetalhe(problema, usuario);
    }

    // RF15/RN09 - arquivar: sai da busca, mas continua no banco
    @Transactional
    public ProblemaDetalheDTO arquivar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Problema problema = buscarAtivo(id);
        Permissoes.exigir(Permissoes.podeEditarProblema(usuario, problema), Permissoes.motivoSemPermissaoDeEdicao(usuario));

        problema.setAtivo(false);
        historicoService.registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_PROBLEMA, problema.getId(), usuario);
        buscaService.remover(problema.getId());
        return paraDetalhe(problema, usuario);
    }

    // RF23 - marcar como aberto ou resolvido
    @Transactional
    public ProblemaDetalheDTO alterarStatus(Long id, StatusProblema status, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Problema problema = buscarAtivo(id);
        Permissoes.exigir(Permissoes.podeEditarProblema(usuario, problema), Permissoes.motivoSemPermissaoDeEdicao(usuario));
        if (problema.getStatus() == status) {
            throw new RegraNegocioException("O problema já está com a situação \"" + status.getRotulo() + "\".");
        }
        problema.setStatus(status);
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_PROBLEMA, problema.getId(), usuario);
        return paraDetalhe(problema, usuario);
    }

    // RF20 - detalhes (todos os perfis podem consultar)
    @Transactional(readOnly = true)
    public ProblemaDetalheDTO detalhar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        return paraDetalhe(buscarEntidade(id), usuario);
    }

    @Transactional(readOnly = true)
    public Problema buscarEntidade(Long id) {
        return problemaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Problema não encontrado."));
    }

    // Problema que ainda pode receber alterações (não arquivado)
    public Problema buscarAtivo(Long id) {
        Problema problema = buscarEntidade(id);
        if (!problema.isAtivo()) {
            throw new RegraNegocioException("Este problema está arquivado e não pode mais ser alterado.");
        }
        return problema;
    }

    private void preencher(Problema problema, ProblemaFormDTO form) {
        problema.setTitulo(form.titulo().trim());
        problema.setDescricao(form.descricao().trim());
        problema.setDepartamento(departamentoComoCampo(form.departamentoId()));
        problema.setCategoria(categoriaComoCampo(form.categoriaId()));
        problema.setPalavrasChave(palavrasChave(form.palavrasChave()));
    }

    // RF07 - normaliza as palavras-chave e reaproveita as existentes
    private Set<PalavraChave> palavrasChave(List<String> termos) {
        Set<String> limpos = new LinkedHashSet<>();
        if (termos != null) {
            for (String termo : termos) {
                if (termo == null || termo.isBlank()) {
                    continue;
                }
                String limpo = termo.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
                if (limpo.length() > TAMANHO_MAXIMO_PALAVRA_CHAVE) {
                    throw new RegraNegocioException("palavrasChave",
                            "Cada palavra-chave pode ter no máximo " + TAMANHO_MAXIMO_PALAVRA_CHAVE + " caracteres.");
                }
                limpos.add(limpo);
            }
        }
        Set<PalavraChave> resultado = new LinkedHashSet<>();
        for (String limpo : limpos) {
            resultado.add(palavraChaveRepository.findByTermo(limpo)
                    .orElseGet(() -> palavraChaveRepository.save(new PalavraChave(limpo))));
        }
        return resultado;
    }

    private Departamento departamentoComoCampo(Long id) {
        try {
            return departamentoService.buscarEntidade(id);
        } catch (RecursoNaoEncontradoException e) {
            throw new RegraNegocioException("departamentoId", "Departamento não encontrado. Escolha um da lista.");
        }
    }

    private Categoria categoriaComoCampo(Long id) {
        try {
            return categoriaService.buscarEntidade(id);
        } catch (RecursoNaoEncontradoException e) {
            throw new RegraNegocioException("categoriaId", "Categoria não encontrada. Escolha uma da lista.");
        }
    }

    private ProblemaDetalheDTO paraDetalhe(Problema p, Usuario usuario) {
        ArvoreCategorias arvore = categoriaService.arvore();

        // RF16 - anexos do problema e de cada solução (sem o conteúdo dos arquivos)
        List<AnexoDTO> anexosDoProblema = new ArrayList<>();
        Map<Long, List<AnexoDTO>> anexosPorSolucao = new HashMap<>();
        if (p.getId() != null) {
            for (AnexoInfo info : anexoRepository.listarDoProblema(p.getId())) {
                AnexoDTO anexo = Conversor.anexo(info, usuario, p.getDepartamento(), p.isAtivo());
                if (info.idSolucao() == null) {
                    anexosDoProblema.add(anexo);
                } else {
                    anexosPorSolucao.computeIfAbsent(info.idSolucao(), id -> new ArrayList<>()).add(anexo);
                }
            }
        }

        // Cap. 6: as soluções validadas aparecem em destaque e no topo da lista;
        // depois vem as que mais ajudaram (RF18) e, por fim, as mais recentes
        List<SolucaoDTO> solucoes = p.getSolucoesAtivas().stream()
                .sorted(Comparator.comparing(Solucao::isValidada).reversed()
                        .thenComparing(Solucao::getTotalAjudou, Comparator.reverseOrder())
                        .thenComparing(Solucao::getDataRegistro, Comparator.reverseOrder()))
                .map(s -> Conversor.solucao(s, usuario, anexosPorSolucao.getOrDefault(s.getId(), List.of())))
                .toList();
        boolean favorito = p.getId() != null
                && favoritoRepository.existsByUsuarioIdAndProblemaId(usuario.getId(), p.getId());
        return new ProblemaDetalheDTO(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getStatus().name(),
                p.getStatus().getRotulo(),
                p.isAtivo(),
                p.getDataRegistro(),
                p.getUsuario().getNome(),
                p.getDepartamento().getId(),
                p.getDepartamento().getNome(),
                p.getCategoria().getId(),
                arvore.caminho(p.getCategoria().getId()),
                p.getPalavrasChave().stream().map(PalavraChave::getTermo).toList(),
                p.getCausasAtivas().stream().map(c -> Conversor.causa(c, usuario)).toList(),
                solucoes,
                p.getId() == null ? List.of() : historicoService.doProblema(p, anexoRepository.idsDoProblema(p.getId())),
                anexosDoProblema,
                favorito,
                p.isAtivo() && Permissoes.podeEditarProblema(usuario, p),
                p.isAtivo() && Permissoes.podeRegistrar(usuario));
    }
}
