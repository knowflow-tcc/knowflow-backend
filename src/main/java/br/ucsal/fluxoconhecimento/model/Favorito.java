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

// Entidade Favorito (RF24)
@Entity
@Table(name = "favorito",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorito_usuario_problema", columnNames = {"id_usuario", "id_problema"}))
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_problema", nullable = false)
    private Problema problema;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public Favorito() {
    }

    public Favorito(Usuario usuario, Problema problema, LocalDateTime dataHora) {
        this.usuario = usuario;
        this.problema = problema;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Problema getProblema() {
        return problema;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
