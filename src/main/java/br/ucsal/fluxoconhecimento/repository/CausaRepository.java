package br.ucsal.fluxoconhecimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Causa;

public interface CausaRepository extends JpaRepository<Causa, Long> {
}
