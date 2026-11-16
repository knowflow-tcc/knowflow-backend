package br.ucsal.fluxoconhecimento.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Entidade Historico (RN10, RF11)
@Entity
@Table(name = "historico",
        indexes = @Index(name = "idx_historico_registro", columnList = "tabela_afetada, id_registro"))
public class Historico {

    public static final String TABELA_USUARIO = "usuario";
    public static final String TABELA_DEPARTAMENTO = "departamento";
    public static final String TABELA_CATEGORIA = "categoria";
    public static final String TABELA_PROBLEMA = "problema";
    public static final String TABELA_CAUSA = "causa";
    public static final String TABELA_SOLUCAO = "solucao";
    public static final String TABELA_COMENTARIO = "comentario";
    public static final String TABELA_ANEXO = "anexo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcaoHistorico acao;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "tabela_afetada", nullable = false, length = 30)
    private String tabelaAfetada;

    @Column(name = "id_registro", nullable = false)
    private Long idRegistro;

    // Quem executou a ação
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public Historico() {
    }

    public Historico(AcaoHistorico acao, String tabelaAfetada, Long idRegistro, Usuario usuario, LocalDateTime dataHora) {
        this.acao = acao;
        this.tabelaAfetada = tabelaAfetada;
        this.idRegistro = idRegistro;
        this.usuario = usuario;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public AcaoHistorico getAcao() {
        return acao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getTabelaAfetada() {
        return tabelaAfetada;
    }

    public Long getIdRegistro() {
        return idRegistro;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
