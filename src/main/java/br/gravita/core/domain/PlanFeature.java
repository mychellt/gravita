package br.gravita.core.domain;

/** A line of a plan's feature list; {@code included = false} renders as a feature the plan lacks. */
public record PlanFeature(String label, boolean included, int displayOrder) {
}
