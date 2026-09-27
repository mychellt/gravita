package br.gravita.core.ports.outbound.security;

import java.time.Instant;

public interface CertificateReaderPort {
	Instant readExpiryDate(byte[] pfxFile, String password);
}
