package br.ucsal.fluxoconhecimento.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.ucsal.fluxoconhecimento.model.Problema;

public interface ProblemaRepository extends JpaRepository<Problema, Long> {

    // Problemas não arquivados (RN09)
    List<Problema> findByAtivoTrue();

    List<Problema> findByAtivoTrueAndDepartamentoId(Long departamentoId);

    // RF22 - problemas registrados no período
    @Query("""
            select p from Problema p
            where p.ativo = true and p.dataRegistro >= :inicio and p.dataRegistro < :fim
            order by p.dataRegistro
            """)
    List<Problema> doPeriodo(LocalDateTime inicio, LocalDateTime fim);

    // RF22 - o mesmo, de um departamento
    @Query("""
            select p from Problema p
            where p.ativo = true and p.departamento.id = :idDepartamento
              and p.dataRegistro >= :inicio and p.dataRegistro < :fim
            order by p.dataRegistro
            """)
    List<Problema> doPeriodoEDepartamento(LocalDateTime inicio, LocalDateTime fim, Long idDepartamento);
}
