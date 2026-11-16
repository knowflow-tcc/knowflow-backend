package br.ucsal.fluxoconhecimento.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.DepartamentoDTO;
import br.ucsal.fluxoconhecimento.dto.DepartamentoFormDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.DepartamentoRepository;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// RF03 - cadastro de departamentos (administrador)
@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;

    public DepartamentoService(DepartamentoRepository departamentoRepository, UsuarioRepository usuarioRepository,
                               HistoricoService historicoService, UsuarioLogadoService usuarioLogado) {
        this.departamentoRepository = departamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<DepartamentoDTO> listar(Long idUsuarioLogado) {
        usuarioLogado.obter(idUsuarioLogado);
        return departamentoRepository.findAllByOrderByNomeAsc().stream().map(this::paraDTO).toList();
    }

    @Transactional
    public DepartamentoDTO cadastrar(DepartamentoFormDTO form, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        String nome = form.nome().trim();
        if (departamentoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um departamento com este nome.");
        }
        Departamento departamento = departamentoRepository.save(new Departamento(nome, limpar(form.descricao())));
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_DEPARTAMENTO, departamento.getId(), admin);
        return paraDTO(departamento);
    }

    @Transactional
    public DepartamentoDTO atualizar(Long id, DepartamentoFormDTO form, Long idUsuarioLogado) {
        Usuario admin = exigirAdministrador(idUsuarioLogado);
        Departamento departamento = buscarEntidade(id);
        String nome = form.nome().trim();
        if (departamentoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe outro departamento com este nome.");
        }
        departamento.setNome(nome);
        departamento.setDescricao(limpar(form.descricao()));
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_DEPARTAMENTO, departamento.getId(), admin);
        return paraDTO(departamento);
    }

    @Transactional(readOnly = true)
    public Departamento buscarEntidade(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Departamento não encontrado."));
    }

    private Usuario exigirAdministrador(Long idUsuarioLogado) {
        Usuario logado = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigirPerfil(logado, Perfil.ADMINISTRADOR);
        return logado;
    }

    private DepartamentoDTO paraDTO(Departamento d) {
        return new DepartamentoDTO(d.getId(), d.getNome(), d.getDescricao(), usuarioRepository.countByDepartamentoId(d.getId()));
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
