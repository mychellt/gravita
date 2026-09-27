package br.gravita.core.ports.outbound.security;

import br.gravita.core.domain.system.UserId;

public interface TotpVerificationPort {

	boolean verify(UserId userId, String code);
}
