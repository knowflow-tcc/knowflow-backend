package br.ucsal.fluxoconhecimento.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.CategoriaDTO;
import br.ucsal.fluxoconhecimento.dto.CategoriaFormDTO;
import br.ucsal.fluxoconhecimento.dto.CategoriaNoDTO;
import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.CategoriaRepository;

// Categorias em árvore (RF07; caso de uso "Gerenciar categorias" - Gestor)
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;
    private final ApplicationEventPublisher eventos;

    public CategoriaService(CategoriaRepository categoriaRepository, HistoricoService historicoService,
                            UsuarioLogadoService usuarioLogado, ApplicationEventPublisher eventos) {
        this.categoriaRepository = categoriaRepository;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
        this.eventos = eventos;
    }

    // Monta a árvore com todas as categorias cadastradas
    @Transactional(readOnly = true)
    public ArvoreCategorias arvore() {
        List<ArvoreCategorias.Entrada> entradas = categoriaRepository.findAll().stream()
                .map(c -> new ArvoreCategorias.Entrada(c.getId(), c.getNome(),
                        c.getCategoriaPai() == null ? null : c.getCategoriaPai().getId()))
                .toList();
        return new ArvoreCategorias(entradas);
    }

    // Lista em pré-ordem (para menus e selects)
    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar(Long idUsuarioLogado) {
        usuarioLogado.obter(idUsuarioLogado);
        ArvoreCategorias arvore = arvore();
        return arvore.percorrerEmPreOrdem().stream()
                .map(no -> new CategoriaDTO(no.getId(), no.getNome(),
                        no.getPai() == null ? null : no.getPai().getId(),
                        no.getNivel(), arvore.caminhoTexto(no.getId()), arvore.caminho(no.getId())))
                .toList();
    }

    // A árvore completa, aninhada (raízes com filhos)
    @Transactional(readOnly = true)
    public List<CategoriaNoDTO> listarArvore(Long idUsuarioLogado) {
        usuarioLogado.obter(idUsuarioLogado);
        ArvoreCategorias arvore = arvore();
        return arvore.raizes().stream().map(no -> paraNoDTO(no, arvore)).toList();
    }

    @Transactional
    public CategoriaDTO cadastrar(CategoriaFormDTO form, Long idUsuarioLogado) {
        Usuario gestor = exigirGestor(idUsuarioLogado);
        ArvoreCategorias arvore = arvore();
        Categoria pai = form.idPai() == null ? null : buscarPai(form.idPai());
        String nome = form.nome().trim();
        garantirNomeUnicoEntreIrmas(arvore, nome, form.idPai(), null);

        Categoria categoria = categoriaRepository.save(new Categoria(nome, pai));
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_CATEGORIA, categoria.getId(), gestor);
        return paraDTO(categoria.getId());
    }

    @Transactional
    public CategoriaDTO atualizar(Long id, CategoriaFormDTO form, Long idUsuarioLogado) {
        Usuario gestor = exigirGestor(idUsuarioLogado);
        Categoria categoria = buscarEntidade(id);
        ArvoreCategorias arvore = arvore();
        if (arvore.criariaCiclo(id, form.idPai())) {
            throw new RegraNegocioException("idPai",
                    "Uma categoria não pode ficar dentro dela mesma nem de uma das suas subcategorias.");
        }
        Categoria pai = form.idPai() == null ? null : buscarPai(form.idPai());
        String nome = form.nome().trim();
        garantirNomeUnicoEntreIrmas(arvore, nome, form.idPai(), id);

        categoria.setNome(nome);
        categoria.setCategoriaPai(pai);
        categoriaRepository.flush();
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_CATEGORIA, categoria.getId(), gestor);
        eventos.publishEvent(new CategoriasAlteradasEvent(id));
        return paraDTO(id);
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    private Categoria buscarPai(Long idPai) {
        return categoriaRepository.findById(idPai)
                .orElseThrow(() -> new RegraNegocioException("idPai", "Categoria superior não encontrada. Escolha uma da lista."));
    }

    // Não deixa repetir nome no mesmo nível da árvore
    private void garantirNomeUnicoEntreIrmas(ArvoreCategorias arvore, String nome, Long idPai, Long idIgnorar) {
        List<ArvoreCategorias.No> irmas = idPai == null || !arvore.contem(idPai)
                ? arvore.raizes()
                : arvore.no(idPai).getFilhos();
        boolean repetida = irmas.stream()
                .anyMatch(no -> no.getNome().equalsIgnoreCase(nome) && (idIgnorar == null || no.getId() != idIgnorar));
        if (repetida) {
            throw new RegraNegocioException("nome", "Já existe uma categoria \"" + nome + "\" neste mesmo nível.");
        }
    }

    private Usuario exigirGestor(Long idUsuarioLogado) {
        Usuario logado = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(logado, Perfil.GESTOR);
        return logado;
    }

    private CategoriaDTO paraDTO(Long id) {
        ArvoreCategorias arvore = arvore();
        ArvoreCategorias.No no = arvore.no(id);
        return new CategoriaDTO(id, no.getNome(), no.getPai() == null ? null : no.getPai().getId(),
                no.getNivel(), arvore.caminhoTexto(id), arvore.caminho(id));
    }

    private static CategoriaNoDTO paraNoDTO(ArvoreCategorias.No no, ArvoreCategorias arvore) {
        return new CategoriaNoDTO(no.getId(), no.getNome(), no.getPai() == null ? null : no.getPai().getId(),
                no.getNivel(), arvore.caminhoTexto(no.getId()),
                no.getFilhos().stream().map(filho -> paraNoDTO(filho, arvore)).toList());
    }
}
