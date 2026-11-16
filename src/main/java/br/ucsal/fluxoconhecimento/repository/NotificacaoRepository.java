package br.ucsal.fluxoconhecimento.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import br.ucsal.fluxoconhecimento.model.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioIdOrderByDataHoraDescIdDesc(Long idUsuario, Pageable pagina);

    long countByUsuarioIdAndLidaFalse(Long idUsuario);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuario.id = :idUsuario and n.lida = false")
    int marcarTodasComoLidas(Long idUsuario);
}
