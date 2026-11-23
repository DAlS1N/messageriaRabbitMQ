package com.atividade.messageria.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoCriadoEvent(
        UUID pedidoId,
        String cliente,
        List<ItemPedido> itens,
        BigDecimal valorTotal,
        Instant criadoEm) {
}
