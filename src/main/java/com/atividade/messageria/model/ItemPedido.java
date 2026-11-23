package com.atividade.messageria.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemPedido(
        @NotBlank String produto,
        @Min(1) int quantidade,
        @NotNull @DecimalMin("0.01") BigDecimal precoUnitario) {
}
