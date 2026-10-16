package br.gravita.adapters.inbound.controllers.inventory.dtos;

import jakarta.validation.constraints.NotNull;

public record SetAllowNegativeStockRequest(@NotNull Boolean allowNegativeStock) {
}
