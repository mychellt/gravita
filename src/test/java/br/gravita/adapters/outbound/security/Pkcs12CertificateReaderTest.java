package br.gravita.adapters.outbound.security;

import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Pkcs12CertificateReaderTest {

	private static final String FIXTURE_PATH = "/certificates/test-a1.pfx";
	private static final String CORRECT_PASSWORD = "gravita-test-pass";

	private final Pkcs12CertificateReader reader = new Pkcs12CertificateReader();

	@Test
	void shouldExtractExpiryDateFromValidPfx() throws Exception {
		byte[] pfxFile = loadFixture();

		Instant expiryDate = reader.readExpiryDate(pfxFile, CORRECT_PASSWORD);

		assertThat(expiryDate).isEqualTo(expectedExpiryFromFixture());
	}

	@Test
	void shouldRejectWrongPassword() throws Exception {
		byte[] pfxFile = loadFixture();

		assertThatThrownBy(() -> reader.readExpiryDate(pfxFile, "not-the-password"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectCorruptFile() {
		byte[] garbage = {1, 2, 3, 4, 5};

		assertThatThrownBy(() -> reader.readExpiryDate(garbage, CORRECT_PASSWORD))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectEmptyFile() {
		assertThatThrownBy(() -> reader.readExpiryDate(new byte[0], CORRECT_PASSWORD))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("required");
	}

	private byte[] loadFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
			return in.readAllBytes();
		}
	}

	private Instant expectedExpiryFromFixture() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new ByteArrayInputStream(loadFixture()), CORRECT_PASSWORD.toCharArray());
		String alias = keyStore.aliases().nextElement();
		return ((X509Certificate) keyStore.getCertificate(alias)).getNotAfter().toInstant();
	}
}
