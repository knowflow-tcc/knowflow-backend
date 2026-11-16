package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

// Notificações do usuário e quantas não foram lidas
public record NotificacoesDTO(long naoLidas, List<NotificacaoDTO> itens) {
}
