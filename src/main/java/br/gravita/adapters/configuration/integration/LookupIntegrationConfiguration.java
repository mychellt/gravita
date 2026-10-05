package br.gravita.adapters.configuration.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LookupIntegrationConfiguration {

	@Bean
	public RestClient cnpjLookupRestClient(
			@Value("${gravita.lookup.cnpj.base-url:https://brasilapi.com.br/api}") final String baseUrl,
			@Value("${gravita.lookup.timeout-ms:2000}") final long timeoutMs) {
		return RestClient.builder().baseUrl(baseUrl).requestFactory(timeoutRequestFactory(timeoutMs)).build();
	}

	@Bean
	public RestClient cepLookupRestClient(
			@Value("${gravita.lookup.cep.base-url:https://viacep.com.br/ws}") final String baseUrl,
			@Value("${gravita.lookup.timeout-ms:2000}") final long timeoutMs) {
		return RestClient.builder().baseUrl(baseUrl).requestFactory(timeoutRequestFactory(timeoutMs)).build();
	}

	private SimpleClientHttpRequestFactory timeoutRequestFactory(final long timeoutMs) {
		final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));
		return factory;
	}
}
