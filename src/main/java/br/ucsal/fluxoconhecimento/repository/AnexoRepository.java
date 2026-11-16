package br.ucsal.fluxoconhecimento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.ucsal.fluxoconhecimento.dto.AnexoInfo;
import br.ucsal.fluxoconhecimento.model.Anexo;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    // Anexos ativos do problema e das soluções, sem o conteúdo do arquivo
    @Query("""
            select new br.ucsal.fluxoconhecimento.dto.AnexoInfo(
                a.id, a.nomeArquivo, a.tipoConteudo, a.tamanho, a.dataHora, u.id, u.nome, p.id, s.id)
            from Anexo a
            join a.usuario u
            left join a.problema p
            left join a.solucao s
            left join s.problema sp
            where a.ativo = true and (p.id = :idProblema or sp.id = :idProblema)
            order by a.id
            """)
    List<AnexoInfo> listarDoProblema(Long idProblema);

    // Ids de todos os anexos do problema, inclusive arquivados
    @Query("""
            select a.id from Anexo a
            left join a.problema p
            left join a.solucao s
            left join s.problema sp
            where p.id = :idProblema or sp.id = :idProblema
            """)
    List<Long> idsDoProblema(Long idProblema);
}
