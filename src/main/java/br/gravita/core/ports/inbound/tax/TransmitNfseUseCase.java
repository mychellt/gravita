package br.gravita.core.ports.inbound.tax;

public interface TransmitNfseUseCase {

	NfseTransmissionResult execute(TransmitNfseCommand command);
}
