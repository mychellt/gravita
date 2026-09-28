package br.gravita.core.ports.inbound.finance;

/**
 * Moves every {@code PENDING} {@code PixCharge} past its {@code expiresAt} to
 * {@code EXPIRED}. Has no REST endpoint; a scheduler triggers it.
 */
public interface ExpirePixChargesUseCase {

	/** @return how many charges were expired */
	int execute();
}
