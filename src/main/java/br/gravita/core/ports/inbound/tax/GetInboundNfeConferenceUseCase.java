package br.gravita.core.ports.inbound.tax;

/** Resolves an inbound NFe against the supplier and product-link registries for the conference screen. */
public interface GetInboundNfeConferenceUseCase {
	InboundNfeConference execute(GetInboundNfeConferenceQuery query);
}
