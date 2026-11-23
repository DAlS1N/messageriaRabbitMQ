package com.atividade.messageria.model;

import java.util.UUID;

public record PedidoStatusEvent(UUID pedidoId, String status, String mensagem) {
}
