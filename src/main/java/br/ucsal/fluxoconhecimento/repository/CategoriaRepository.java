package br.ucsal.fluxoconhecimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
