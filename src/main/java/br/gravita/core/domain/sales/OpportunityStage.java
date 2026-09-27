package br.gravita.core.domain.sales;

public enum OpportunityStage {
	PROSPECTING,
	PROPOSAL,
	NEGOTIATION,
	CLOSED,
	LOST;

	public boolean isTerminal() {
		return this == CLOSED || this == LOST;
	}
}
