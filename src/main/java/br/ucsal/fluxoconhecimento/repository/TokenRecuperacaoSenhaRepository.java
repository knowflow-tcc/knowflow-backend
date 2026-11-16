package br.ucsal.fluxoconhecimento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import br.ucsal.fluxoconhecimento.model.TokenRecuperacaoSenha;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    Optional<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    // Invalida os links anteriores do usuário
    @Modifying
    @Query("update TokenRecuperacaoSenha t set t.usado = true where t.usuario.id = :idUsuario and t.usado = false")
    int invalidarPendentes(Long idUsuario);
}
