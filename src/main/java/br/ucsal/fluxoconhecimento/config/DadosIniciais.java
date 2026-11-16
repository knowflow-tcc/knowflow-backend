package br.ucsal.fluxoconhecimento.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.HistoricoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// Cria o administrador inicial na primeira execução (RF02)
@Component
@Order(1)
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final UsuarioRepository usuarioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final HistoricoRepository historicoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${knowflow.admin.nome}")
    private String nome;
    @Value("${knowflow.admin.email}")
    private String email;
    @Value("${knowflow.admin.senha}")
    private String senha;
    @Value("${knowflow.admin.departamento:Administração}")
    private String nomeDepartamento;

    public DadosIniciais(UsuarioRepository usuarioRepository, DepartamentoRepository departamentoRepository,
                         HistoricoRepository historicoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.departamentoRepository = departamentoRepository;
        this.historicoRepository = historicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Departamento departamento = departamentoRepository.save(
                new Departamento(nomeDepartamento, "Criado automaticamente na primeira execução."));
        Usuario admin = usuarioRepository.save(new Usuario(nome, email.toLowerCase(),
                passwordEncoder.encode(senha), Perfil.ADMINISTRADOR, departamento));

        LocalDateTime agora = LocalDateTime.now();
        historicoRepository.save(new Historico(AcaoHistorico.INCLUSAO, Historico.TABELA_DEPARTAMENTO, departamento.getId(), admin, agora));
        historicoRepository.save(new Historico(AcaoHistorico.INCLUSAO, Historico.TABELA_USUARIO, admin.getId(), admin, agora));
        log.warn("Administrador inicial criado: {} (troque a senha após o primeiro acesso)", email);
    }
}
