package br.gravita.core.ports.inbound.tax;

public interface CancelNfseUseCase {

	void execute(CancelNfseCommand command);
}
