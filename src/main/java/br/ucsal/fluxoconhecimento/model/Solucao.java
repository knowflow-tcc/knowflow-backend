package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

// Entidade Solucao (RF06, RF10)
@Entity
@Table(name = "solucao")
public class Solucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String descricao;

    @Column(nullable = false)
    private boolean validada = false;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    // RN05: só existe vinculada a um problema
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_problema", nullable = false)
    private Problema problema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean ativo = true;

    // RF17 - comentários feitos na solução
    @OneToMany(mappedBy = "solucao")
    @OrderBy("id")
    private List<Comentario> comentarios = new ArrayList<>();

    // RF18 - votos "esta solução me ajudou"
    @OneToMany(mappedBy = "solucao")
    private List<AvaliacaoSolucao> avaliacoes = new ArrayList<>();

    public Solucao() {
    }

    public Solucao(String descricao, Problema problema, Usuario usuario, LocalDateTime dataRegistro) {
        this.descricao = descricao;
        this.problema = problema;
        this.usuario = usuario;
        this.dataRegistro = dataRegistro;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isValidada() {
        return validada;
    }

    public void setValidada(boolean validada) {
        this.validada = validada;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public Problema getProblema() {
        return problema;
    }

    public void setProblema(Problema problema) {
        this.problema = problema;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<Comentario> getComentarios() {
        return comentarios;
    }

    // Comentários não arquivados
    public List<Comentario> getComentariosAtivos() {
        return comentarios.stream().filter(Comentario::isAtivo).toList();
    }

    public List<AvaliacaoSolucao> getAvaliacoes() {
        return avaliacoes;
    }

    // RF18 - quantas pessoas disseram que a solução ajudou
    public long getTotalAjudou() {
        return avaliacoes.stream().filter(AvaliacaoSolucao::isAjudou).count();
    }

    public long getTotalNaoAjudou() {
        return avaliacoes.size() - getTotalAjudou();
    }
}
