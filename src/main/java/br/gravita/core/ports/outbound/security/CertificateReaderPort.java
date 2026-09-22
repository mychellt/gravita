package br.gravita.core.ports.outbound.security;

import java.time.Instant;

/**
 * Reads a PKCS12 (.pfx) keystore to obtain its certificate's expiry date, consumed by {@code
 * UploadDigitalCertificateUseCase}. Implementations raise {@code BusinessRuleException} when the
 * payload isn't a valid {@code .pfx} or the password doesn't unlock it.
 */
public interface CertificateReaderPort {
	Instant readExpiryDate(byte[] pfxFile, String password);
}
