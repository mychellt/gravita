package br.gravita.core.ports.inbound.inventory;

/** The stock header figures: active products (SKUs) and how many of them hold less than their registered minimum. */
public record InventorySummary(int activeSkus, int belowMinimum) {
}
