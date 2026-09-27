package br.gravita.adapters.outbound.security;

import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import br.gravita.core.ports.outbound.tax.SupervisorAuthorizationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SupervisorAuthorizationAdapter implements SupervisorAuthorizationPort {

	private final PasswordVerificationPort passwordVerificationPort;
	private final String supervisorPasswordHash;

	public SupervisorAuthorizationAdapter(PasswordVerificationPort passwordVerificationPort,
			@Value("${gravita.pdv.supervisor-password-hash}") String supervisorPasswordHash) {
		this.passwordVerificationPort = passwordVerificationPort;
		this.supervisorPasswordHash = supervisorPasswordHash;
	}

	@Override
	public boolean authorize(String supervisorCredential) {
		return supervisorCredential != null && !supervisorCredential.isBlank()
				&& passwordVerificationPort.matches(supervisorCredential, supervisorPasswordHash);
	}
}
