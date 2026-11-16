package br.ucsal.fluxoconhecimento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.AvaliacaoSolucao;

public interface AvaliacaoSolucaoRepository extends JpaRepository<AvaliacaoSolucao, Long> {

    Optional<AvaliacaoSolucao> findBySolucaoIdAndUsuarioId(Long idSolucao, Long idUsuario);
}
