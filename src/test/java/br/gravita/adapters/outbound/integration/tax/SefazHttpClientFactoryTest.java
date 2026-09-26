package br.gravita.adapters.outbound.integration.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SefazHttpClientFactoryTest {

	private static final String FIXTURE_PATH = "/certificates/test-a1.pfx";
	private static final String CORRECT_PASSWORD = "gravita-test-pass";

	private final SefazHttpClientFactory factory = new SefazHttpClientFactory();

	@Test
	void buildsAMutualTlsRestClientFromAValidCertificate() throws Exception {
		RestClient client = factory.build(loadFixture(), CORRECT_PASSWORD, "https://localhost:0", Duration.ofSeconds(1));

		assertThat(client).isNotNull();
	}

	@Test
	void rejectsTheWrongCertificatePassword() throws Exception {
		byte[] pfxFile = loadFixture();

		assertThatThrownBy(() -> factory.build(pfxFile, "not-the-password", "https://localhost:0", Duration.ofSeconds(1)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsACorruptCertificateFile() {
		byte[] garbage = {1, 2, 3, 4, 5};

		assertThatThrownBy(() -> factory.build(garbage, CORRECT_PASSWORD, "https://localhost:0", Duration.ofSeconds(1)))
				.isInstanceOf(BusinessRuleException.class);
	}

	private byte[] loadFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
			return in.readAllBytes();
		}
	}
}
