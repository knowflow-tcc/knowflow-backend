package br.ucsal.fluxoconhecimento.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.ucsal.fluxoconhecimento.model.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    Optional<Favorito> findByUsuarioIdAndProblemaId(Long idUsuario, Long idProblema);

    boolean existsByUsuarioIdAndProblemaId(Long idUsuario, Long idProblema);

    // Ids dos favoritos do usuário
    @Query("select f.problema.id from Favorito f where f.usuario.id = :idUsuario")
    Set<Long> idsDosProblemasFavoritos(Long idUsuario);

    List<Favorito> findByUsuarioIdOrderByDataHoraDesc(Long idUsuario);
}
