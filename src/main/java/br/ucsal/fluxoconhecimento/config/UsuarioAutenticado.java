package br.ucsal.fluxoconhecimento.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Usuario;

// Usuário guardado na sessão após o login (RF01)
public class UsuarioAutenticado implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private final boolean ativo;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.perfil = usuario.getPerfil();
        this.ativo = usuario.isAtivo();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
