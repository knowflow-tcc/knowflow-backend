package br.ucsal.fluxoconhecimento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Departamento> findAllByOrderByNomeAsc();
}
