package br.gravita.adapters.outbound.security;

import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import br.gravita.core.ports.outbound.tax.SupervisorAuthorizationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * UC-M3-07: checks the PDV cancellation password against a single configured
 * bcrypt hash, reusing {@link PasswordVerificationPort} the same way {@code
 * AuthenticateService} does. Masterdata has no "supervisor" user/role concept
 * yet - only a generic permission system keyed by module/screen/action - so
 * this is a shop-wide shared credential rather than a per-supervisor one
 * until that concept exists; flagged to the tech lead alongside this PR,
 * same as {@code IssueNfceService}'s {@code ISSUER_STATE} note.
 */
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
