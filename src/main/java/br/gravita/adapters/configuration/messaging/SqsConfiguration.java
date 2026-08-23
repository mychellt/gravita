package br.gravita.adapters.configuration.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class SqsConfiguration {

	@Bean
	public SqsClient sqsClient(
			@Value("${aws.region}") String region,
			@Value("${aws.sqs.endpoint:}") String endpoint,
			@Value("${aws.access-key:test}") String accessKey,
			@Value("${aws.secret-key:test}") String secretKey) {
		SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));

		if (!endpoint.isBlank()) {
			builder.endpointOverride(URI.create(endpoint))
					.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}

		return builder.build();
	}
}
