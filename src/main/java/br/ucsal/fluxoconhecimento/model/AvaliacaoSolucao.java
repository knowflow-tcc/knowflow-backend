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
import jakarta.persistence.UniqueConstraint;

// Entidade AvaliacaoSolucao (RF18): "esta solução me ajudou"
@Entity
@Table(name = "avaliacao_solucao",
        uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_usuario_solucao", columnNames = {"id_usuario", "id_solucao"}))
public class AvaliacaoSolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean ajudou;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solucao", nullable = false)
    private Solucao solucao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public AvaliacaoSolucao() {
    }

    public AvaliacaoSolucao(Solucao solucao, Usuario usuario, boolean ajudou, LocalDateTime dataHora) {
        this.solucao = solucao;
        this.usuario = usuario;
        this.ajudou = ajudou;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public boolean isAjudou() {
        return ajudou;
    }

    public void setAjudou(boolean ajudou) {
        this.ajudou = ajudou;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Solucao getSolucao() {
        return solucao;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
