package br.gravita.adapters.outbound.security;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.outbound.security.CertificateReaderPort;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Optional;

/**
 * Opens the uploaded PKCS12 ({@code .pfx}) keystore with the JDK's own provider to obtain the
 * signing certificate's expiry date, mirroring how {@code CredentialCipher}/{@code
 * PasswordHasher} keep low-level JDK crypto APIs at the adapter boundary rather than in core.
 */
@Component
public class Pkcs12CertificateReader implements CertificateReaderPort {

	@Override
	public Instant readExpiryDate(byte[] pfxFile, String password) {
		if (pfxFile == null || pfxFile.length == 0) {
			throw new BusinessRuleException("Certificate file is required");
		}
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			char[] passwordChars = password == null ? new char[0] : password.toCharArray();
			keyStore.load(new ByteArrayInputStream(pfxFile), passwordChars);

			X509Certificate certificate = findFirstCertificate(keyStore)
					.orElseThrow(() -> new BusinessRuleException("Certificate file contains no certificate entry"));

			return certificate.getNotAfter().toInstant();
		} catch (BusinessRuleException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessRuleException("Invalid certificate file or wrong password");
		}
	}

	private Optional<X509Certificate> findFirstCertificate(KeyStore keyStore) throws GeneralSecurityException {
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			Certificate certificate = keyStore.getCertificate(aliases.nextElement());
			if (certificate instanceof X509Certificate x509Certificate) {
				return Optional.of(x509Certificate);
			}
		}
		return Optional.empty();
	}
}
