package br.ucsal.fluxoconhecimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
}
