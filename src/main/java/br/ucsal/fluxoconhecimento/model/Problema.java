package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

// Entidade Problema
@Entity
@Table(name = "problema")
public class Problema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 5000)
    private String descricao;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProblema status = StatusProblema.ABERTO;

    @Column(nullable = false)
    private boolean ativo = true;

    // Autor: quem registrou ("um usuário pode registrar vários problemas")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToMany
    @JoinTable(name = "problema_palavra_chave",
            joinColumns = @JoinColumn(name = "id_problema"),
            inverseJoinColumns = @JoinColumn(name = "id_palavra_chave"))
    @OrderBy("termo")
    private Set<PalavraChave> palavrasChave = new LinkedHashSet<>();

    @OneToMany(mappedBy = "problema")
    @OrderBy("id")
    private List<Causa> causas = new ArrayList<>();

    @OneToMany(mappedBy = "problema")
    @OrderBy("id")
    private List<Solucao> solucoes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public StatusProblema getStatus() {
        return status;
    }

    public void setStatus(StatusProblema status) {
        this.status = status;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Set<PalavraChave> getPalavrasChave() {
        return palavrasChave;
    }

    public void setPalavrasChave(Set<PalavraChave> palavrasChave) {
        this.palavrasChave = palavrasChave;
    }

    public List<Causa> getCausas() {
        return causas;
    }

    public List<Solucao> getSolucoes() {
        return solucoes;
    }

    // Causas não arquivadas
    public List<Causa> getCausasAtivas() {
        return causas.stream().filter(Causa::isAtivo).toList();
    }

    // Soluções não arquivadas
    public List<Solucao> getSolucoesAtivas() {
        return solucoes.stream().filter(Solucao::isAtivo).toList();
    }
}
