package br.gravita.core.ports.inbound.sales;

/** One opportunity of the funnel with the names of its customer and owner ({@code null} when unknown). */
public record OpportunityListItem(OpportunityView opportunity, String customerName, String ownerName) {
}
