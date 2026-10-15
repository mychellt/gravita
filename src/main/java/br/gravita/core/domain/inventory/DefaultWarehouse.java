package br.gravita.core.domain.inventory;

import java.util.UUID;

/** There is a single implicit warehouse for now: no warehouse table exists, stock rows just carry this id. */
public final class DefaultWarehouse {

	public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private DefaultWarehouse() {
	}
}
