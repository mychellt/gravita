package br.gravita.adapters.outbound.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsEmailNotificationAdapterTest {

	private static final String QUEUE_URL = "http://localhost/queue";

	@Mock
	private SqsClient sqsClient;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	private JsonNode publishedMessage() {
		ArgumentCaptor<SendMessageRequest> request = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqsClient).sendMessage(request.capture());
		assertThat(request.getValue().queueUrl()).isEqualTo(QUEUE_URL);
		return objectMapper.readTree(request.getValue().messageBody());
	}

	@Test
	@DisplayName("A plain e-mail keeps its original message shape, without an HTML part")
	void shouldKeepThePlainMessageShape() {
		new SqsEmailNotificationAdapter(sqsClient, objectMapper, QUEUE_URL).send("a@b.com", "Subject", "Body");

		JsonNode message = publishedMessage();
		assertThat(message.get("to").asString()).isEqualTo("a@b.com");
		assertThat(message.get("subject").asString()).isEqualTo("Subject");
		assertThat(message.get("body").asString()).isEqualTo("Body");
		assertThat(message.has("htmlBody")).isFalse();
	}

	@Test
	@DisplayName("An HTML e-mail carries the HTML in htmlBody and the plain-text fallback in body")
	void shouldCarryHtmlAndPlainTextParts() {
		new SqsEmailNotificationAdapter(sqsClient, objectMapper, QUEUE_URL)
				.sendHtml("a@b.com", "Subject", "<p>Hi</p>", "Hi");

		JsonNode message = publishedMessage();
		assertThat(message.get("htmlBody").asString()).isEqualTo("<p>Hi</p>");
		assertThat(message.get("body").asString()).isEqualTo("Hi");
	}
}
