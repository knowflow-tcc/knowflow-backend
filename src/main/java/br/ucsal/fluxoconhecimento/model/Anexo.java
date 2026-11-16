package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Entidade Anexo (RF16): pertence a um problema ou a uma solução
@Entity
@Table(name = "anexo")
// RNF07: o anexo pertence a um problema OU a uma solução - nunca aos dois, nunca a nenhum
@Check(name = "ck_anexo_problema_ou_solucao", constraints = "(id_problema is null) <> (id_solucao is null)")
public class Anexo {

    // Limite do arquivo (o mesmo de spring.servlet.multipart.max-file-size)
    public static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(nullable = false)
    private long tamanho;

    // Só é lida no download
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] conteudo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_problema")
    private Problema problema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solucao")
    private Solucao solucao;

    // Quem anexou
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public Anexo() {
    }

    public Anexo(String nomeArquivo, String tipoConteudo, byte[] conteudo, Usuario usuario, LocalDateTime dataHora) {
        this.nomeArquivo = nomeArquivo;
        this.tipoConteudo = tipoConteudo;
        this.conteudo = conteudo;
        this.tamanho = conteudo.length;
        this.usuario = usuario;
        this.dataHora = dataHora;
    }

    // Problema a que o anexo pertence (direto ou pela solução)
    public Problema getProblemaDeOrigem() {
        return problema != null ? problema : solucao.getProblema();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public long getTamanho() {
        return tamanho;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Problema getProblema() {
        return problema;
    }

    public void setProblema(Problema problema) {
        this.problema = problema;
    }

    public Solucao getSolucao() {
        return solucao;
    }

    public void setSolucao(Solucao solucao) {
        this.solucao = solucao;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
