package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.messaging.FiscalDocumentEmailRequest;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes a fiscal-document e-mail (UC-M2-03, AC5) onto the same queue-
 * based delivery mechanism as {@link SqsEmailNotificationAdapter}, with the
 * XML/DANFE attachments base64-encoded in the message body - the downstream
 * mail sender is outside this repo either way, so this only needs to carry
 * enough to let it build the actual e-mail.
 */
@Component
public class SqsFiscalDocumentEmailAdapter implements SendFiscalDocumentByEmailPort {

	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;
	private final String queueUrl;

	public SqsFiscalDocumentEmailAdapter(SqsClient sqsClient, ObjectMapper objectMapper,
			@Value("${aws.sqs.email-queue-url}") String queueUrl) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
		this.queueUrl = queueUrl;
	}

	@Override
	public void send(FiscalDocumentEmailRequest request) {
		String messageBody = objectMapper.writeValueAsString(Map.of(
				"to", request.to(),
				"subject", request.subject(),
				"body", request.body(),
				"xmlFilename", request.xmlFilename(),
				"xmlBase64", Base64.getEncoder().encodeToString(request.xmlContent()),
				"danfeFilename", request.danfeFilename(),
				"danfeBase64", Base64.getEncoder().encodeToString(request.danfeContent())));
		sqsClient.sendMessage(SendMessageRequest.builder()
				.queueUrl(queueUrl)
				.messageBody(messageBody)
				.build());
	}
}
