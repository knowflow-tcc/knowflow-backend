package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Entidade Notificacao (RF21)
@Entity
@Table(name = "notificacao",
        indexes = @Index(name = "idx_notificacao_usuario", columnList = "id_usuario, lida"))
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida = false;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Destinatário: o autor da solução
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Problema ao qual o aviso leva quando clicado
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_problema", nullable = false)
    private Problema problema;

    public Notificacao() {
    }

    public Notificacao(Usuario usuario, Problema problema, String mensagem, LocalDateTime dataHora) {
        this.usuario = usuario;
        this.problema = problema;
        this.mensagem = mensagem;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public String getMensagem() {
        return mensagem;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Problema getProblema() {
        return problema;
    }
}
