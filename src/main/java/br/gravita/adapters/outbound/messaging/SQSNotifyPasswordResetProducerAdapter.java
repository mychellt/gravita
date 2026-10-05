package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.NotifyPasswordResetProducerPort;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SQSNotifyPasswordResetProducerAdapter implements NotifyPasswordResetProducerPort {

    private final SqsPublisher sqsPublisher;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public SQSNotifyPasswordResetProducerAdapter(
            final SqsPublisher sqsPublisher,
            final ObjectMapper objectMapper,
            @Value("${aws.sqs.password-reset-queue-name}") final String queueName) {
        this.sqsPublisher = sqsPublisher;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    @Override
    @SneakyThrows
    public Void execute(final Context context) {
        final var message = context.getData(NotifyPasswordResetMessage.class);
        sqsPublisher.publish(queueName, objectMapper.writeValueAsString(message));

        return null;
    }
}
