package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.FiltroBuscaDTO;
import br.ucsal.fluxoconhecimento.dto.ResultadoBuscaDTO;
import br.ucsal.fluxoconhecimento.dto.SemelhanteDTO;
import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.estrutura.IndiceBuscaHash;
import br.ucsal.fluxoconhecimento.estrutura.NormalizadorTexto;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.PalavraChave;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.repository.FavoritoRepository;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;

// Busca (RF08, RF19) e semelhantes (RF09) usando a tabela hash e a árvore de categorias.
// O índice fica em memória: montado ao iniciar e atualizado a cada cadastro, edição ou arquivamento.
// Pesos: título 3, palavras-chave 2, demais campos 1.
@Service
public class BuscaService {

    static final int PESO_TITULO = 3;
    static final int PESO_PALAVRA_CHAVE = 2;
    static final int PESO_DESCRICAO = 1;
    static final int PESO_CATEGORIA = 1;
    static final int PESO_CAUSA_SOLUCAO = 1;

    // Abaixo disso a sugestão de semelhantes vira ruído
    static final int SEMELHANCA_MINIMA_SUGESTAO = 20;
    // Semelhança mínima para contar como o mesmo problema (painel)
    static final int SEMELHANCA_RECORRENCIA = 50;
    static final int LIMITE_RESULTADOS = 200;
    static final int LIMITE_SEMELHANTES = 5;

    private static final Logger log = LoggerFactory.getLogger(BuscaService.class);

    private final IndiceBuscaHash indice = new IndiceBuscaHash();
    private final ProblemaRepository problemaRepository;
    private final CategoriaService categoriaService;
    private final UsuarioLogadoService usuarioLogado;
    private final FavoritoRepository favoritoRepository;

    public BuscaService(ProblemaRepository problemaRepository, CategoriaService categoriaService,
                        UsuarioLogadoService usuarioLogado, FavoritoRepository favoritoRepository) {
        this.problemaRepository = problemaRepository;
        this.categoriaService = categoriaService;
        this.usuarioLogado = usuarioLogado;
        this.favoritoRepository = favoritoRepository;
    }

    // ------------------------------------------------------------------
    // Manutenção do índice
    // ------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void reconstruirIndice() {
        indice.limpar();
        ArvoreCategorias arvore = categoriaService.arvore();
        problemaRepository.findByAtivoTrue().forEach(p -> indice.indexar(p.getId(), pesosDosTermos(p, arvore)));
        log.info("Índice de busca montado: {} problemas, {} termos distintos",
                indice.totalDocumentos(), indice.totalTermos());
    }

    // Nomes de categorias fazem parte do índice: se a árvore mudou, reindexa
    @EventListener
    @Transactional(readOnly = true)
    public void aoAlterarCategorias(CategoriasAlteradasEvent evento) {
        reconstruirIndice();
    }

    // Indexa (ou reindexa) um problema; problemas arquivados saem do índice
    public void indexar(Problema problema) {
        if (!problema.isAtivo()) {
            indice.remover(problema.getId());
            return;
        }
        indice.indexar(problema.getId(), pesosDosTermos(problema, categoriaService.arvore()));
    }

    public void remover(Long idProblema) {
        indice.remover(idProblema);
    }

    public int totalIndexados() {
        return indice.totalDocumentos();
    }

    static Map<String, Integer> pesosDosTermos(Problema p, ArvoreCategorias arvore) {
        Map<String, Integer> pesos = new HashMap<>();
        somar(pesos, p.getTitulo(), PESO_TITULO);
        for (PalavraChave palavra : p.getPalavrasChave()) {
            somar(pesos, palavra.getTermo(), PESO_PALAVRA_CHAVE);
        }
        somar(pesos, p.getDescricao(), PESO_DESCRICAO);
        for (String parte : arvore.caminho(p.getCategoria().getId())) {
            somar(pesos, parte, PESO_CATEGORIA);
        }
        for (Causa causa : p.getCausasAtivas()) {
            somar(pesos, causa.getDescricao(), PESO_CAUSA_SOLUCAO);
        }
        for (Solucao solucao : p.getSolucoesAtivas()) {
            somar(pesos, solucao.getDescricao(), PESO_CAUSA_SOLUCAO);
        }
        return pesos;
    }

    private static void somar(Map<String, Integer> pesos, String texto, int peso) {
        for (String termo : NormalizadorTexto.termos(texto)) {
            pesos.merge(termo, peso, Integer::sum);
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    // RF08/RF19 - busca com filtros e ordenação; RF24 - filtro de favoritos
    @Transactional(readOnly = true)
    public List<ResultadoBuscaDTO> buscar(FiltroBuscaDTO filtro, Long idUsuarioLogado) {
        Long idUsuario = usuarioLogado.obter(idUsuarioLogado).getId();
        Set<Long> favoritos = favoritoRepository.idsDosProblemasFavoritos(idUsuario);
        ArvoreCategorias arvore = categoriaService.arvore();
        List<String> termos = NormalizadorTexto.termos(filtro.texto());
        boolean comTexto = !termos.isEmpty();

        // 1) Candidatos: pela tabela hash (quando há texto) ou todos os problemas ativos
        Map<Long, IndiceBuscaHash.Resultado> relevancia = new HashMap<>();
        List<Problema> candidatos;
        if (comTexto) {
            indice.buscar(termos, Integer.MAX_VALUE, null).forEach(r -> relevancia.put(r.id(), r));
            candidatos = problemaRepository.findAllById(relevancia.keySet());
        } else {
            candidatos = problemaRepository.findByAtivoTrue();
        }

        // 2) Filtros; a categoria inclui toda a subárvore (percurso na árvore)
        Set<Long> categorias = filtro.categoriaId() == null ? null : arvore.idsDaSubarvore(filtro.categoriaId());
        LocalDateTime inicio = filtro.de() == null ? null : filtro.de().atStartOfDay();
        LocalDateTime fim = filtro.ate() == null ? null : filtro.ate().plusDays(1).atStartOfDay();
        Stream<Problema> resultado = candidatos.stream()
                .filter(Problema::isAtivo)
                .filter(p -> !filtro.somenteFavoritos() || favoritos.contains(p.getId()))
                .filter(p -> categorias == null || categorias.contains(p.getCategoria().getId()))
                .filter(p -> filtro.departamentoId() == null || filtro.departamentoId().equals(p.getDepartamento().getId()))
                .filter(p -> filtro.status() == null || filtro.status() == p.getStatus())
                .filter(p -> inicio == null || !p.getDataRegistro().isBefore(inicio))
                .filter(p -> fim == null || p.getDataRegistro().isBefore(fim));

        // 3) Ordenação
        String ordem = filtro.ordem() == null || filtro.ordem().isBlank()
                ? (comTexto ? "relevancia" : "recentes")
                : filtro.ordem();
        Comparator<Problema> maisRecente = Comparator.comparing(Problema::getDataRegistro).reversed();
        Comparator<Problema> comparador = switch (ordem) {
            case "antigos" -> Comparator.comparing(Problema::getDataRegistro);
            case "relevancia" -> comTexto
                    ? Comparator.comparingDouble((Problema p) -> relevancia.get(p.getId()).pontuacao()).reversed()
                            .thenComparing(maisRecente)
                    : maisRecente;
            default -> maisRecente;
        };

        return resultado.sorted(comparador)
                .limit(LIMITE_RESULTADOS)
                .map(p -> paraResultado(p, relevancia.get(p.getId()), arvore, favoritos.contains(p.getId())))
                .toList();
    }

    // RF09 - problemas parecidos com o texto digitado no cadastro
    @Transactional(readOnly = true)
    public List<SemelhanteDTO> semelhantes(String texto, Long ignorarId, Long idUsuarioLogado) {
        usuarioLogado.obter(idUsuarioLogado);
        List<String> termos = NormalizadorTexto.termos(texto);
        if (termos.isEmpty()) {
            return List.of();
        }
        List<IndiceBuscaHash.Resultado> encontrados = indice.buscar(termos, LIMITE_SEMELHANTES * 4, ignorarId).stream()
                .filter(r -> r.semelhanca() >= SEMELHANCA_MINIMA_SUGESTAO)
                .limit(LIMITE_SEMELHANTES)
                .toList();
        Map<Long, Problema> porId = problemaRepository
                .findAllById(encontrados.stream().map(IndiceBuscaHash.Resultado::id).toList()).stream()
                .collect(Collectors.toMap(Problema::getId, Function.identity()));
        ArvoreCategorias arvore = categoriaService.arvore();

        return encontrados.stream()
                .filter(r -> porId.containsKey(r.id()) && porId.get(r.id()).isAtivo())
                .map(r -> {
                    Problema p = porId.get(r.id());
                    String validada = p.getSolucoesAtivas().stream()
                            .filter(Solucao::isValidada)
                            .map(s -> resumir(s.getDescricao(), 300))
                            .findFirst().orElse(null);
                    return new SemelhanteDTO(p.getId(), p.getTitulo(), p.getStatus().name(), p.getStatus().getRotulo(),
                            arvore.caminhoTexto(p.getCategoria().getId()), r.semelhanca(), r.termosEmComum(), validada);
                })
                .toList();
    }

    // RF13 - quantos registros são praticamente o mesmo problema
    public Map<Long, Integer> contarRecorrencias(Collection<Problema> problemas) {
        Set<Long> conjunto = problemas.stream().map(Problema::getId).collect(Collectors.toSet());
        Map<Long, Integer> recorrencias = new HashMap<>();
        for (Problema p : problemas) {
            List<String> termos = NormalizadorTexto.termos(p.getTitulo() + " " + p.getDescricao());
            long parecidos = indice.buscar(termos, Integer.MAX_VALUE, p.getId()).stream()
                    .filter(r -> r.semelhanca() >= SEMELHANCA_RECORRENCIA && conjunto.contains(r.id()))
                    .count();
            if (parecidos > 0) {
                recorrencias.put(p.getId(), (int) parecidos);
            }
        }
        return recorrencias;
    }

    private ResultadoBuscaDTO paraResultado(Problema p, IndiceBuscaHash.Resultado relevancia, ArvoreCategorias arvore,
                                           boolean favorito) {
        List<Solucao> solucoes = p.getSolucoesAtivas();
        return new ResultadoBuscaDTO(
                p.getId(),
                p.getTitulo(),
                resumir(p.getDescricao(), 200),
                p.getStatus().name(),
                p.getStatus().getRotulo(),
                p.getDataRegistro(),
                p.getDepartamento().getNome(),
                arvore.caminhoTexto(p.getCategoria().getId()),
                p.getUsuario().getNome(),
                solucoes.size(),
                solucoes.stream().anyMatch(Solucao::isValidada),
                p.getPalavrasChave().stream().map(PalavraChave::getTermo).toList(),
                relevancia == null ? null : relevancia.semelhanca(),
                relevancia == null ? List.of() : relevancia.termosEmComum(),
                favorito);
    }

    static String resumir(String texto, int limite) {
        if (texto == null || texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite).trim() + "…";
    }
}
