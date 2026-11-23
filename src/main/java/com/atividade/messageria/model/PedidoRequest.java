package com.atividade.messageria.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PedidoRequest(
        @NotBlank String cliente,
        @NotEmpty @Valid List<ItemPedido> itens) {
}
