package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.messaging.EmailNotificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class SqsEmailNotificationAdapter implements EmailNotificationPort {

	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;
	private final String queueUrl;

	public SqsEmailNotificationAdapter(SqsClient sqsClient, ObjectMapper objectMapper,
			@Value("${aws.sqs.email-queue-url}") String queueUrl) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
		this.queueUrl = queueUrl;
	}

	@Override
	public void send(String to, String subject, String body) {
		String messageBody = objectMapper.writeValueAsString(Map.of("to", to, "subject", subject, "body", body));
		sqsClient.sendMessage(SendMessageRequest.builder()
				.queueUrl(queueUrl)
				.messageBody(messageBody)
				.build());
	}
}
