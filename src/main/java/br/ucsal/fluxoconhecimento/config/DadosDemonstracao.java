package br.ucsal.fluxoconhecimento.config;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.AvaliacaoSolucao;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Comentario;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.PalavraChave;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.AvaliacaoSolucaoRepository;
import br.ucsal.fluxoconhecimento.repository.CategoriaRepository;
import br.ucsal.fluxoconhecimento.repository.CausaRepository;
import br.ucsal.fluxoconhecimento.repository.ComentarioRepository;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.HistoricoRepository;
import br.ucsal.fluxoconhecimento.repository.PalavraChaveRepository;
import br.ucsal.fluxoconhecimento.repository.ProblemaRepository;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// Dados de demonstração (apresentação e testes manuais das telas)
@Component
@Order(2)
@ConditionalOnProperty(name = "knowflow.demo", havingValue = "true")
public class DadosDemonstracao implements CommandLineRunner {

    static final String SENHA = "Knowflow2026";
    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);

    private final UsuarioRepository usuarioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProblemaRepository problemaRepository;
    private final CausaRepository causaRepository;
    private final SolucaoRepository solucaoRepository;
    private final PalavraChaveRepository palavraChaveRepository;
    private final ComentarioRepository comentarioRepository;
    private final AvaliacaoSolucaoRepository avaliacaoRepository;
    private final HistoricoRepository historicoRepository;
    private final PasswordEncoder passwordEncoder;

    public DadosDemonstracao(UsuarioRepository usuarioRepository, DepartamentoRepository departamentoRepository,
                             CategoriaRepository categoriaRepository, ProblemaRepository problemaRepository,
                             CausaRepository causaRepository, SolucaoRepository solucaoRepository,
                             PalavraChaveRepository palavraChaveRepository, ComentarioRepository comentarioRepository,
                             AvaliacaoSolucaoRepository avaliacaoRepository, HistoricoRepository historicoRepository,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.departamentoRepository = departamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.problemaRepository = problemaRepository;
        this.causaRepository = causaRepository;
        this.solucaoRepository = solucaoRepository;
        this.palavraChaveRepository = palavraChaveRepository;
        this.comentarioRepository = comentarioRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.historicoRepository = historicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (problemaRepository.count() > 0) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();

        Departamento ti = departamento("Tecnologia da Informação", "Suporte, redes e sistemas.");
        Departamento rh = departamento("Recursos Humanos", "Pessoas, benefícios e férias.");
        Departamento financeiro = departamento("Financeiro", "Pagamentos, notas fiscais e reembolsos.");

        Usuario ana = usuario("Ana Souza", "gestor.ti@knowflow.com", Perfil.GESTOR, ti);
        Usuario carlos = usuario("Carlos Lima", "carlos@knowflow.com", Perfil.COLABORADOR, ti);
        Usuario beatriz = usuario("Beatriz Rocha", "beatriz@knowflow.com", Perfil.COLABORADOR, ti);
        Usuario marina = usuario("Marina Alves", "gestor.rh@knowflow.com", Perfil.GESTOR, rh);
        Usuario pedro = usuario("Pedro Santos", "pedro@knowflow.com", Perfil.COLABORADOR, financeiro);

        Categoria infraestrutura = categoria("Infraestrutura", null);
        Categoria redes = categoria("Redes e Internet", infraestrutura);
        Categoria impressoras = categoria("Impressoras", infraestrutura);
        Categoria sistemas = categoria("Sistemas", null);
        Categoria erp = categoria("ERP", sistemas);
        Categoria email = categoria("E-mail", sistemas);
        Categoria processos = categoria("Processos internos", null);
        Categoria ferias = categoria("Férias e benefícios", processos);
        Categoria reembolso = categoria("Reembolso", processos);

        // 1) Resolvido, com solução validada, votos e comentário
        Problema vpn = problema("VPN não conecta fora da empresa",
                "Ao tentar conectar na VPN de casa aparece \"falha na autenticação\". Dentro da empresa a rede funciona normalmente.",
                carlos, ti, redes, agora.minusMonths(4).minusDays(3), "vpn", "acesso remoto", "certificado");
        causa(vpn, "O certificado digital da VPN instalado no notebook expirou.", beatriz);
        Solucao vpnSolucao = solucao(vpn, "1. Abrir o portal de certificados (intranet > TI > Certificados).\n"
                + "2. Baixar o novo certificado da VPN.\n3. Importar no cliente da VPN e reconectar.", beatriz, vpn.getDataRegistro().plusHours(2));
        validar(vpnSolucao, ana);
        voto(vpnSolucao, carlos, true);
        voto(vpnSolucao, pedro, true);
        comentario(vpnSolucao, "Funcionou aqui também. Precisei reiniciar o notebook depois de importar.", pedro);

        // 2 e 3) Problemas parecidos (aparecem como recorrentes no painel)
        Problema impressora = problema("Impressora do 2º andar não imprime",
                "Os documentos ficam parados na fila de impressão e a impressora do 2º andar não imprime nada.",
                beatriz, ti, impressoras, agora.minusMonths(2).minusDays(10), "impressora", "fila de impressão");
        causa(impressora, "Um documento corrompido trava a fila de impressão.", carlos);
        Solucao filaSolucao = solucao(impressora, "Reiniciar o serviço \"Spooler de impressão\" no servidor e excluir o documento travado da fila.",
                carlos, impressora.getDataRegistro().plusDays(1));
        voto(filaSolucao, beatriz, true);
        problema("Impressora do segundo andar não imprime documentos",
                "Mandei imprimir e a impressora do segundo andar não imprime; o documento fica na fila de impressão.",
                pedro, ti, impressoras, agora.minusDays(12), "impressora");

        // 4) Resolvido sem validação (marcado pelo autor)
        Problema celular = problema("E-mail não sincroniza no celular",
                "Depois da troca de senha o e-mail corporativo parou de sincronizar no celular.",
                carlos, ti, email, agora.minusMonths(1).minusDays(5), "e-mail", "celular", "senha");
        causa(celular, "O aplicativo continua usando a senha antiga salva.", carlos);
        solucao(celular, "Remover a conta do aplicativo de e-mail e adicioná-la de novo com a senha nova.", carlos,
                celular.getDataRegistro().plusHours(5));
        celular.setStatus(StatusProblema.RESOLVIDO);

        // 5) Aberto, só com causa
        Problema nota = problema("Erro ao emitir nota fiscal no ERP",
                "Ao emitir a nota fiscal o ERP mostra \"rejeição 539: duplicidade de NF-e\".",
                pedro, financeiro, erp, agora.minusDays(20), "nota fiscal", "erp");
        causa(nota, "A numeração da nota pulou após uma queda de conexão durante a emissão anterior.", pedro);

        // 6) RH - gestora registra e valida
        Problema feriasProblema = problema("Como solicitar férias pelo portal",
                "Colaboradores novos não encontram onde pedir férias no portal do RH.",
                marina, rh, ferias, agora.minusMonths(3), "férias", "portal do rh");
        Solucao feriasSolucao = solucao(feriasProblema, "Portal do RH > Meus dados > Férias > Nova solicitação. "
                + "O pedido precisa ser feito com 30 dias de antecedência.", marina, feriasProblema.getDataRegistro().plusHours(1));
        validar(feriasSolucao, marina);
        voto(feriasSolucao, pedro, true);

        // 7 e 8) Abertos
        problema("Reembolso de despesas recusado sem motivo",
                "O pedido de reembolso de táxi foi recusado no sistema, mas não aparece o motivo.",
                pedro, financeiro, reembolso, agora.minusDays(4), "reembolso");
        Problema lenta = problema("Internet lenta na sala de reuniões",
                "Nas chamadas de vídeo da sala de reuniões a internet fica lenta e o vídeo trava.",
                beatriz, ti, redes, agora.minusDays(2), "internet", "wi-fi", "videochamada");
        causa(lenta, "Muitos dispositivos conectados no mesmo ponto de acesso Wi-Fi.", ana);

        log.warn("Dados de demonstração criados. Usuários: gestor.ti@knowflow.com, carlos@knowflow.com, "
                + "beatriz@knowflow.com, gestor.rh@knowflow.com, pedro@knowflow.com (senha {})", SENHA);
    }

    private Departamento departamento(String nome, String descricao) {
        return departamentoRepository.save(new Departamento(nome, descricao));
    }

    private Usuario usuario(String nome, String email, Perfil perfil, Departamento departamento) {
        return usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(SENHA), perfil, departamento));
    }

    private Categoria categoria(String nome, Categoria pai) {
        return categoriaRepository.save(new Categoria(nome, pai));
    }

    private Problema problema(String titulo, String descricao, Usuario autor, Departamento departamento,
                              Categoria categoria, LocalDateTime data, String... palavras) {
        Problema p = new Problema();
        p.setTitulo(titulo);
        p.setDescricao(descricao);
        p.setUsuario(autor);
        p.setDepartamento(departamento);
        p.setCategoria(categoria);
        p.setDataRegistro(data);
        p.setStatus(StatusProblema.ABERTO);
        Set<PalavraChave> termos = new LinkedHashSet<>();
        for (String termo : palavras) {
            termos.add(palavraChaveRepository.findByTermo(termo).orElseGet(() -> palavraChaveRepository.save(new PalavraChave(termo))));
        }
        p.setPalavrasChave(termos);
        problemaRepository.save(p);
        historico(AcaoHistorico.INCLUSAO, Historico.TABELA_PROBLEMA, p.getId(), autor, data);
        return p;
    }

    private void causa(Problema problema, String descricao, Usuario autor) {
        Causa c = causaRepository.save(new Causa(descricao, problema, autor));
        problema.getCausas().add(c);
        historico(AcaoHistorico.INCLUSAO, Historico.TABELA_CAUSA, c.getId(), autor, problema.getDataRegistro().plusMinutes(30));
    }

    private Solucao solucao(Problema problema, String descricao, Usuario autor, LocalDateTime data) {
        Solucao s = solucaoRepository.save(new Solucao(descricao, problema, autor, data));
        problema.getSolucoes().add(s);
        historico(AcaoHistorico.INCLUSAO, Historico.TABELA_SOLUCAO, s.getId(), autor, data);
        return s;
    }

    private void validar(Solucao solucao, Usuario gestor) {
        solucao.setValidada(true);
        solucao.getProblema().setStatus(StatusProblema.RESOLVIDO);
        historico(AcaoHistorico.VALIDACAO, Historico.TABELA_SOLUCAO, solucao.getId(), gestor, solucao.getDataRegistro().plusDays(1));
    }

    private void voto(Solucao solucao, Usuario usuario, boolean ajudou) {
        solucao.getAvaliacoes().add(avaliacaoRepository.save(
                new AvaliacaoSolucao(solucao, usuario, ajudou, solucao.getDataRegistro().plusDays(2))));
    }

    private void comentario(Solucao solucao, String texto, Usuario autor) {
        LocalDateTime data = solucao.getDataRegistro().plusDays(2);
        Comentario c = comentarioRepository.save(new Comentario(texto, solucao, autor, data));
        solucao.getComentarios().add(c);
        historico(AcaoHistorico.INCLUSAO, Historico.TABELA_COMENTARIO, c.getId(), autor, data);
    }

    private void historico(AcaoHistorico acao, String tabela, Long id, Usuario usuario, LocalDateTime data) {
        historicoRepository.save(new Historico(acao, tabela, id, usuario, data));
    }
}
