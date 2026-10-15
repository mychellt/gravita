package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.PageQuery;
import java.util.UUID;

/** {@code kind} picks which stock units are listed; lots when omitted. */
public record ListLotsQuery(UUID productId, Kind kind, PageQuery pageQuery) {

	public enum Kind {
		LOT, SERIAL
	}

	public ListLotsQuery {
		if (kind == null) {
			kind = Kind.LOT;
		}
	}
}
