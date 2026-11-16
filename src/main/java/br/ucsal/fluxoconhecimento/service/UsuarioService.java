package br.ucsal.fluxoconhecimento.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.UsuarioDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioFormDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioLogadoDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// Gestão de usuários (RF02, RF12)
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository, DepartamentoRepository departamentoRepository,
                          PasswordEncoder passwordEncoder, HistoricoService historicoService,
                          UsuarioLogadoService usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.departamentoRepository = departamentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
    }

    // Dados de quem está logado
    @Transactional(readOnly = true)
    public UsuarioLogadoDTO usuarioLogado(Long idUsuarioLogado) {
        return Conversor.usuarioLogado(usuarioLogado.obter(idUsuarioLogado));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar(Long idUsuarioLogado) {
        exigirAdministrador(idUsuarioLogado);
        return usuarioRepository.findAllByOrderByNomeAsc().stream().map(Conversor::usuario).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscar(Long id, Long idUsuarioLogado) {
        exigirAdministrador(idUsuarioLogado);
        return Conversor.usuario(buscarEntidade(id));
    }

    @Transactional
    public UsuarioDTO cadastrar(UsuarioFormDTO form, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        String email = normalizarEmail(form.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("email", "Já existe um usuário com este e-mail. Use outro e-mail.");
        }
        PoliticaSenha.validar(form.senha());

        Usuario usuario = new Usuario(form.nome().trim(), email, passwordEncoder.encode(form.senha()),
                form.perfil(), buscarDepartamento(form.departamentoId()));
        usuarioRepository.save(usuario);
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_USUARIO, usuario.getId(), admin);
        return Conversor.usuario(usuario);
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioFormDTO form, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(form.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RegraNegocioException("email", "Já existe outro usuário com este e-mail. Use outro e-mail.");
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR && form.perfil() != Perfil.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo(usuario);
        }
        if (form.senha() != null && !form.senha().isBlank()) {
            PoliticaSenha.validar(form.senha());
            usuario.setSenhaHash(passwordEncoder.encode(form.senha()));
        }
        usuario.setNome(form.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(form.perfil());
        usuario.setDepartamento(buscarDepartamento(form.departamentoId()));
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_USUARIO, usuario.getId(), admin);
        return Conversor.usuario(usuario);
    }

    // RF02 - desativar sem perder o que o usuário registrou
    @Transactional
    public UsuarioDTO desativar(Long id, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        Usuario usuario = buscarEntidade(id);
        if (usuario.getId().equals(admin.getId())) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("Este usuário já está desativado.");
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo(usuario);
        }
        usuario.setAtivo(false);
        historicoService.registrar(AcaoHistorico.DESATIVACAO, Historico.TABELA_USUARIO, usuario.getId(), admin);
        return Conversor.usuario(usuario);
    }

    @Transactional
    public UsuarioDTO reativar(Long id, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        Usuario usuario = buscarEntidade(id);
        if (usuario.isAtivo()) {
            throw new RegraNegocioException("Este usuário já está ativo.");
        }
        usuario.setAtivo(true);
        historicoService.registrar(AcaoHistorico.REATIVACAO, Historico.TABELA_USUARIO, usuario.getId(), admin);
        return Conversor.usuario(usuario);
    }

    // RNF02 (LGPD) - apaga os dados pessoais de um usuário que saiu da empresa
    @Transactional
    public UsuarioDTO anonimizar(Long id, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        Usuario usuario = buscarEntidade(id);
        if (usuario.isAtivo()) {
            throw new RegraNegocioException("Desative o usuário antes de anonimizar os dados pessoais.");
        }
        usuario.setNome("Usuário anonimizado #" + usuario.getId());
        usuario.setEmail("anonimizado-" + usuario.getId() + "@knowflow.invalid");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        historicoService.registrar(AcaoHistorico.ANONIMIZACAO, Historico.TABELA_USUARIO, usuario.getId(), admin);
        return Conversor.usuario(usuario);
    }

    private Usuario exigirAdministrador(Long idUsuarioLogado) {
        Usuario logado = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(logado, Perfil.ADMINISTRADOR);
        return logado;
    }

    // Não deixa o sistema ficar sem administrador ativo
    private void garantirOutroAdministradorAtivo(Usuario usuario) {
        long adminsAtivos = usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR);
        if (usuario.isAtivo() && adminsAtivos <= 1) {
            throw new RegraNegocioException("perfil",
                    "Este é o único administrador ativo. Cadastre ou promova outro administrador antes.");
        }
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Departamento buscarDepartamento(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("departamentoId", "Departamento não encontrado. Escolha um da lista."));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
