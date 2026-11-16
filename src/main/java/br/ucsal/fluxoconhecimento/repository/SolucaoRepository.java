package br.ucsal.fluxoconhecimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Solucao;

public interface SolucaoRepository extends JpaRepository<Solucao, Long> {
}
