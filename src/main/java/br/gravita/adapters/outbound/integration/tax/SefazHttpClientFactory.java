package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds a {@link RestClient} authenticated with a company's A1 certificate
 * as its mutual-TLS client credential - the "certificate-based" part of
 * UC-M3-04's SEFAZ-UF client adapter. Kept separate from
 * {@link SefazUfSubmissionAdapter} so the certificate/TLS plumbing can be
 * exercised in isolation without a live SEFAZ endpoint.
 */
class SefazHttpClientFactory {

	RestClient build(byte[] pfxPayload, String password, String baseUrl, Duration timeout) {
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(new ByteArrayInputStream(pfxPayload), password.toCharArray());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password.toCharArray());

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).connectTimeout(timeout).build();
			JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
			requestFactory.setReadTimeout(timeout);

			return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
		} catch (GeneralSecurityException | IOException e) {
			throw new BusinessRuleException("Invalid digital certificate for SEFAZ mutual TLS: " + e.getMessage());
		}
	}
}
