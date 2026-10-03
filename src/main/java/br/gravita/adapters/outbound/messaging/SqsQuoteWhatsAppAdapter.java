package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.messaging.SendQuoteByWhatsAppPort;
import br.gravita.core.ports.messaging.records.SendQuoteByWhatsAppRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * UC-M7-02: publishes a quote WhatsApp delivery onto the same queue-based
 * mechanism as {@link SqsFiscalDocumentEmailAdapter} - the downstream
 * WhatsApp Business API integration is outside this repo, so this only
 * needs to carry enough to let it build the actual message.
 */
@Component
public class SqsQuoteWhatsAppAdapter implements SendQuoteByWhatsAppPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsQuoteWhatsAppAdapter(SqsClient sqsClient, ObjectMapper objectMapper,
                                   @Value("${aws.sqs.whatsapp-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void send(SendQuoteByWhatsAppRequest request) {
        String messageBody = objectMapper.writeValueAsString(Map.of(
                "phoneNumber", request.phoneNumber(),
                "quoteId", request.quoteId().toString(),
                "totalValue", request.totalValue().toString(),
                "validUntil", request.validUntil().toString()));
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());
    }
}
