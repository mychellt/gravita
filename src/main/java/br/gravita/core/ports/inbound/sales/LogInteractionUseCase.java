package br.gravita.core.ports.inbound.sales;

public interface LogInteractionUseCase {
	InteractionView execute(LogInteractionCommand command);
}
