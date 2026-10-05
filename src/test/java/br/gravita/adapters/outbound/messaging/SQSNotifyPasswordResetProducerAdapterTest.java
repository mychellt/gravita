package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSNotifyPasswordResetProducerAdapterTest {

    private static final String QUEUE_NAME = "password_reset_queue";
    private static final String QUEUE_URL = "http://localhost:4566/000000000000/password_reset_queue";

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private SQSNotifyPasswordResetProducerAdapter adapter() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build());
        return new SQSNotifyPasswordResetProducerAdapter(new SqsPublisher(sqsClient), objectMapper, QUEUE_NAME);
    }

    private Context reset() {
        return new Context(NotifyPasswordResetMessage.builder()
                .token("tok-123").username("ana").recipient("ana@acme.com").tenantId(UUID.randomUUID()).build());
    }

    @Test
    @DisplayName("Publishes the password reset message to the password_reset_queue queue")
    void shouldPublishToThePasswordResetQueue() {
        adapter().execute(reset());

        final ArgumentCaptor<GetQueueUrlRequest> lookup = ArgumentCaptor.forClass(GetQueueUrlRequest.class);
        verify(sqsClient).getQueueUrl(lookup.capture());
        assertThat(lookup.getValue().queueName()).isEqualTo(QUEUE_NAME);

        final ArgumentCaptor<SendMessageRequest> sent = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(sent.capture());
        assertThat(sent.getValue().queueUrl()).isEqualTo(QUEUE_URL);
        final JsonNode body = objectMapper.readTree(sent.getValue().messageBody());
        assertThat(body.get("token").asString()).isEqualTo("tok-123");
        assertThat(body.get("username").asString()).isEqualTo("ana");
        assertThat(body.get("recipient").asString()).isEqualTo("ana@acme.com");
    }

    @Test
    @DisplayName("Looks the queue URL up only once across several messages")
    void shouldCacheTheQueueUrl() {
        final var adapter = adapter();

        adapter.execute(reset());
        adapter.execute(reset());

        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }
}
