package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.HistoricoDTO;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Comentario;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.HistoricoRepository;

// RN10 - registra toda inclusão, alteração ou arquivamento
@Service
public class HistoricoService {

    public static final int LIMITE_PADRAO = 300;

    private final HistoricoRepository historicoRepository;
    private final UsuarioLogadoService usuarioLogado;

    public HistoricoService(HistoricoRepository historicoRepository, UsuarioLogadoService usuarioLogado) {
        this.historicoRepository = historicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public void registrar(AcaoHistorico acao, String tabelaAfetada, Long idRegistro, Usuario usuario) {
        historicoRepository.save(new Historico(acao, tabelaAfetada, idRegistro, usuario, LocalDateTime.now()));
    }

    // Histórico do problema e de tudo ligado a ele
    @Transactional(readOnly = true)
    public List<HistoricoDTO> doProblema(Problema problema, List<Long> idsAnexos) {
        List<Historico> entradas = new ArrayList<>(historicoRepository
                .findByTabelaAfetadaAndIdRegistroIn(Historico.TABELA_PROBLEMA, List.of(problema.getId())));
        List<Long> idsCausas = problema.getCausas().stream().map(Causa::getId).toList();
        List<Long> idsSolucoes = problema.getSolucoes().stream().map(Solucao::getId).toList();
        List<Long> idsComentarios = problema.getSolucoes().stream()
                .flatMap(s -> s.getComentarios().stream()).map(Comentario::getId).toList();
        adicionar(entradas, Historico.TABELA_CAUSA, idsCausas);
        adicionar(entradas, Historico.TABELA_SOLUCAO, idsSolucoes);
        adicionar(entradas, Historico.TABELA_COMENTARIO, idsComentarios);
        adicionar(entradas, Historico.TABELA_ANEXO, idsAnexos);
        return entradas.stream()
                .sorted(Comparator.comparing(Historico::getDataHora).thenComparing(Historico::getId).reversed())
                .map(Conversor::historico)
                .toList();
    }

    private void adicionar(List<Historico> entradas, String tabela, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            entradas.addAll(historicoRepository.findByTabelaAfetadaAndIdRegistroIn(tabela, ids));
        }
    }

    // Histórico geral de ações
    @Transactional(readOnly = true)
    public List<HistoricoDTO> listarGeral(String tabelaAfetada, Long idUsuarioLogado) {
        Usuario logado = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(logado, Perfil.ADMINISTRADOR);
        PageRequest pagina = PageRequest.of(0, LIMITE_PADRAO);
        List<Historico> entradas = (tabelaAfetada == null || tabelaAfetada.isBlank())
                ? historicoRepository.findAllByOrderByDataHoraDescIdDesc(pagina)
                : historicoRepository.findByTabelaAfetadaOrderByDataHoraDescIdDesc(tabelaAfetada, pagina);
        return entradas.stream().map(Conversor::historico).toList();
    }
}
