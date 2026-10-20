package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

	private final SessionStorePort sessionStorePort;

	@Override
	public void execute(final String sessionToken) {
		if (sessionToken != null && !sessionToken.isBlank()) {
			sessionStorePort.revoke(sessionToken);
		}
	}
}
