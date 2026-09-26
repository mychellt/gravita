package br.gravita.core.ports.outbound.tax;

import java.util.Objects;

public record SefazSubmissionResult(String protocol) {

	public SefazSubmissionResult {
		Objects.requireNonNull(protocol, "protocol");
	}
}
