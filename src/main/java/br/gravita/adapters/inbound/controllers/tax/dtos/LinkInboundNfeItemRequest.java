package br.gravita.adapters.inbound.controllers.tax.dtos;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LinkInboundNfeItemRequest(@NotNull UUID productId) {
}
