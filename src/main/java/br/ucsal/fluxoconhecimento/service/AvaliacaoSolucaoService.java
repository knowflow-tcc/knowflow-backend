package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AvaliacaoSolucao;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.AvaliacaoSolucaoRepository;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;

// RF18 - o usuário indica se uma solução o ajudou
@Service
public class AvaliacaoSolucaoService {

    private final AvaliacaoSolucaoRepository avaliacaoRepository;
    private final SolucaoRepository solucaoRepository;
    private final UsuarioLogadoService usuarioLogado;

    public AvaliacaoSolucaoService(AvaliacaoSolucaoRepository avaliacaoRepository, SolucaoRepository solucaoRepository,
                                   UsuarioLogadoService usuarioLogado) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.solucaoRepository = solucaoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public SolucaoDTO avaliar(Long idSolucao, boolean ajudou, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Solucao solucao = solucaoRepository.findById(idSolucao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solução não encontrada."));
        if (!solucao.isAtivo() || !solucao.getProblema().isAtivo()) {
            throw new RegraNegocioException("Esta solução (ou o seu problema) está arquivada e não pode ser avaliada.");
        }
        Permissoes.exigir(Permissoes.podeAvaliar(usuario, solucao),
                Permissoes.podeRegistrar(usuario)
                        ? "Você não pode avaliar uma solução que você mesmo registrou."
                        : "Somente colaboradores e gestores avaliam soluções.");

        AvaliacaoSolucao avaliacao = avaliacaoRepository.findBySolucaoIdAndUsuarioId(solucao.getId(), usuario.getId())
                .orElse(null);
        if (avaliacao == null) {
            avaliacao = avaliacaoRepository.save(new AvaliacaoSolucao(solucao, usuario, ajudou, LocalDateTime.now()));
            solucao.getAvaliacoes().add(avaliacao);
        } else {
            avaliacao.setAjudou(ajudou);
            avaliacao.setDataHora(LocalDateTime.now());
        }
        return Conversor.solucao(solucao, usuario);
    }
}
