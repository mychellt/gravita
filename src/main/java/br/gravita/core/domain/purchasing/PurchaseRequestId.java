package br.gravita.core.domain.purchasing;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PurchaseRequestId(UUID value) {
    public static PurchaseRequestId of(UUID value) {
        return new PurchaseRequestId(value);
    }
}
