package br.ucsal.fluxoconhecimento.dto;

import br.ucsal.fluxoconhecimento.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Entrada de cadastro/edição de usuário (RF02)
public record UsuarioFormDTO(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.") String nome,
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido, por exemplo nome@empresa.com.")
        @Size(max = 150, message = "O e-mail pode ter no máximo 150 caracteres.") String email,
        @NotNull(message = "Escolha o perfil.") Perfil perfil,
        @NotNull(message = "Escolha o departamento.") Long departamentoId,
        String senha) {
}
