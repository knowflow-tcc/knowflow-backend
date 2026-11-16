package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Entidade TokenRecuperacaoSenha (RF14): link de uso único
@Entity
@Table(name = "token_recuperacao_senha")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean usado = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public TokenRecuperacaoSenha() {
    }

    public TokenRecuperacaoSenha(String tokenHash, Usuario usuario, LocalDateTime expiraEm) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEm = expiraEm;
    }

    public boolean valido(LocalDateTime agora) {
        return !usado && agora.isBefore(expiraEm);
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
