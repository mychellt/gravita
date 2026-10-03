package br.gravita.adapters.outbound.messaging;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SqsPublisher {

    private final SqsClient sqsClient;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();

    public SqsPublisher(final SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public void publish(final String queueName, final String messageBody) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrls.computeIfAbsent(queueName, this::resolveQueueUrl))
                .messageBody(messageBody)
                .build());
    }

    private String resolveQueueUrl(final String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }
}
