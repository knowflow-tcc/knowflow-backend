package br.ucsal.fluxoconhecimento.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.ItemContagemDTO;
import br.ucsal.fluxoconhecimento.dto.PainelDTO;
import br.ucsal.fluxoconhecimento.estrutura.ArvoreCategorias;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.PalavraChave;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;

// RF13 - painel com indicadores (Gestor e Administrador)
@Service
public class PainelService {

    static final int MESES_EVOLUCAO = 6;
    static final int TAMANHO_RANKING = 5;
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM/yyyy", new Locale("pt", "BR"));

    private final ProblemaRepository problemaRepository;
    private final DepartamentoService departamentoService;
    private final CategoriaService categoriaService;
    private final BuscaService buscaService;
    private final UsuarioLogadoService usuarioLogado;

    public PainelService(ProblemaRepository problemaRepository, DepartamentoService departamentoService,
                         CategoriaService categoriaService, BuscaService buscaService,
                         UsuarioLogadoService usuarioLogado) {
        this.problemaRepository = problemaRepository;
        this.departamentoService = departamentoService;
        this.categoriaService = categoriaService;
        this.buscaService = buscaService;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public PainelDTO gerar(Long idDepartamentoFiltro, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(usuario, Perfil.GESTOR, Perfil.ADMINISTRADOR);

        Departamento departamento = usuario.getPerfil() == Perfil.GESTOR
                ? usuario.getDepartamento()
                : (idDepartamentoFiltro == null ? null : departamentoService.buscarEntidade(idDepartamentoFiltro));
        List<Problema> problemas = departamento == null
                ? problemaRepository.findByAtivoTrue()
                : problemaRepository.findByAtivoTrueAndDepartamentoId(departamento.getId());

        long abertos = problemas.stream().filter(p -> p.getStatus() == StatusProblema.ABERTO).count();
        long resolvidos = problemas.size() - abertos;
        List<Solucao> solucoes = problemas.stream().flatMap(p -> p.getSolucoesAtivas().stream()).toList();
        long validadas = solucoes.stream().filter(Solucao::isValidada).count();
        int taxaResolucao = problemas.isEmpty() ? 0 : (int) Math.round(100.0 * resolvidos / problemas.size());

        return new PainelDTO(
                departamento == null ? null : departamento.getId(),
                departamento == null ? "Todos os departamentos" : departamento.getNome(),
                problemas.size(), abertos, resolvidos, taxaResolucao,
                solucoes.size(), validadas,
                problemasRecorrentes(problemas),
                categoriasComMaisProblemas(problemas),
                palavrasChaveMaisUsadas(problemas),
                evolucaoMensal(problemas, YearMonth.now()),
                departamento == null ? abertosPorDepartamento(problemas) : List.of(),
                solucoesMaisUteis(solucoes));
    }

    // Problemas que mais se repetem
    private List<ItemContagemDTO> problemasRecorrentes(List<Problema> problemas) {
        Map<Long, Integer> recorrencias = buscaService.contarRecorrencias(problemas);
        return problemas.stream()
                .filter(p -> recorrencias.containsKey(p.getId()))
                .sorted(Comparator.comparing((Problema p) -> recorrencias.get(p.getId())).reversed()
                        .thenComparing(Problema::getId, Comparator.reverseOrder()))
                .limit(TAMANHO_RANKING)
                .map(p -> new ItemContagemDTO(p.getTitulo(), recorrencias.get(p.getId()), p.getId()))
                .toList();
    }

    // RF13 - soluções mais reutilizadas (RF18)
    private List<ItemContagemDTO> solucoesMaisUteis(List<Solucao> solucoes) {
        return solucoes.stream()
                .filter(s -> s.getTotalAjudou() > 0)
                .sorted(Comparator.comparing(Solucao::getTotalAjudou).reversed()
                        .thenComparing(Solucao::getId, Comparator.reverseOrder()))
                .limit(TAMANHO_RANKING)
                .map(s -> new ItemContagemDTO(BuscaService.resumir(s.getDescricao(), 80) + " (" + s.getProblema().getTitulo() + ")",
                        s.getTotalAjudou(), s.getProblema().getId()))
                .toList();
    }

    // Problemas por categoria principal
    private List<ItemContagemDTO> categoriasComMaisProblemas(List<Problema> problemas) {
        ArvoreCategorias arvore = categoriaService.arvore();
        Map<String, Long> porCategoria = new HashMap<>();
        for (Problema p : problemas) {
            List<String> caminho = arvore.caminho(p.getCategoria().getId());
            String raiz = caminho.isEmpty() ? p.getCategoria().getNome() : caminho.get(0);
            porCategoria.merge(raiz, 1L, Long::sum);
        }
        return ranking(porCategoria, TAMANHO_RANKING);
    }

    private List<ItemContagemDTO> palavrasChaveMaisUsadas(List<Problema> problemas) {
        Map<String, Long> contagem = new HashMap<>();
        for (Problema p : problemas) {
            for (PalavraChave palavra : p.getPalavrasChave()) {
                contagem.merge(palavra.getTermo(), 1L, Long::sum);
            }
        }
        return ranking(contagem, 8);
    }

    // Registros por mês, incluindo meses sem registro
    static List<ItemContagemDTO> evolucaoMensal(List<Problema> problemas, YearMonth mesAtual) {
        TreeMap<YearMonth, Long> porMes = new TreeMap<>();
        YearMonth primeiro = mesAtual.minusMonths(MESES_EVOLUCAO - 1L);
        for (int i = 0; i < MESES_EVOLUCAO; i++) {
            porMes.put(primeiro.plusMonths(i), 0L);
        }
        for (Problema p : problemas) {
            YearMonth mes = YearMonth.from(p.getDataRegistro());
            porMes.computeIfPresent(mes, (m, total) -> total + 1);
        }
        return porMes.entrySet().stream()
                .map(e -> new ItemContagemDTO(MES.format(e.getKey()), e.getValue()))
                .toList();
    }

    private List<ItemContagemDTO> abertosPorDepartamento(List<Problema> problemas) {
        Map<String, Long> contagem = new HashMap<>();
        problemas.stream()
                .filter(p -> p.getStatus() == StatusProblema.ABERTO)
                .forEach(p -> contagem.merge(p.getDepartamento().getNome(), 1L, Long::sum));
        return ranking(contagem, TAMANHO_RANKING);
    }

    private static List<ItemContagemDTO> ranking(Map<String, Long> contagem, int limite) {
        return contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limite)
                .map(e -> new ItemContagemDTO(e.getKey(), e.getValue()))
                .toList();
    }
}
