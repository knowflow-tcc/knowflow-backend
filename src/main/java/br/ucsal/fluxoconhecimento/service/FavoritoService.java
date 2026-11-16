package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.model.Favorito;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.FavoritoRepository;

// RF24 - favoritos
@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final ProblemaService problemaService;
    private final UsuarioLogadoService usuarioLogado;

    public FavoritoService(FavoritoRepository favoritoRepository, ProblemaService problemaService,
                           UsuarioLogadoService usuarioLogado) {
        this.favoritoRepository = favoritoRepository;
        this.problemaService = problemaService;
        this.usuarioLogado = usuarioLogado;
    }

    // Marca (se ainda não estiver marcado)
    @Transactional
    public void marcar(Long idProblema, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Problema problema = problemaService.buscarEntidade(idProblema);
        if (!favoritoRepository.existsByUsuarioIdAndProblemaId(usuario.getId(), problema.getId())) {
            favoritoRepository.save(new Favorito(usuario, problema, LocalDateTime.now()));
        }
    }

    // Desmarca (se estiver marcado)
    @Transactional
    public void desmarcar(Long idProblema, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        favoritoRepository.findByUsuarioIdAndProblemaId(usuario.getId(), idProblema)
                .ifPresent(favoritoRepository::delete);
    }

    @Transactional(readOnly = true)
    public Set<Long> idsFavoritos(Long idUsuario) {
        return favoritoRepository.idsDosProblemasFavoritos(idUsuario);
    }
}
