package br.ucsal.fluxoconhecimento.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ucsal.fluxoconhecimento.model.Historico;

public interface HistoricoRepository extends JpaRepository<Historico, Long> {

    // Histórico de vários registros da mesma tabela
    List<Historico> findByTabelaAfetadaAndIdRegistroIn(String tabelaAfetada, Collection<Long> idsRegistro);

    // Histórico geral, do mais recente ao mais antigo
    List<Historico> findAllByOrderByDataHoraDescIdDesc(Pageable pagina);

    List<Historico> findByTabelaAfetadaOrderByDataHoraDescIdDesc(String tabelaAfetada, Pageable pagina);
}
