package br.ucsal.fluxoconhecimento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.PalavraChave;

public interface PalavraChaveRepository extends JpaRepository<PalavraChave, Long> {

    Optional<PalavraChave> findByTermo(String termo);
}
