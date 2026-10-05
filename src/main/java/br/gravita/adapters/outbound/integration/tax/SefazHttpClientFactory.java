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

class SefazHttpClientFactory {

	RestClient build(final byte[] pfxPayload, final String password, final String baseUrl, final Duration timeout) {
		try {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(new ByteArrayInputStream(pfxPayload), password.toCharArray());

			final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password.toCharArray());

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			final HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).connectTimeout(timeout).build();
			final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
			requestFactory.setReadTimeout(timeout);

			return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
		} catch (GeneralSecurityException | IOException e) {
			throw new BusinessRuleException("Invalid digital certificate for SEFAZ mutual TLS: " + e.getMessage());
		}
	}
}
