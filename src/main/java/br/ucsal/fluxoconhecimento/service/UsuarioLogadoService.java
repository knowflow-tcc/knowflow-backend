package br.ucsal.fluxoconhecimento.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.UsuarioRepository;

// Carrega do banco o usuário que fez a requisição
@Service
public class UsuarioLogadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Usuario obter(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new AcessoNegadoException("Sessão inválida. Entre novamente no sistema."));
        if (!usuario.isAtivo()) {
            throw new AcessoNegadoException("Seu usuário está desativado. Procure o administrador.");
        }
        return usuario;
    }
}
