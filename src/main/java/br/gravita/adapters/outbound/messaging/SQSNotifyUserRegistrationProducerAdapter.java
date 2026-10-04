package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SQSNotifyUserRegistrationProducerAdapter implements NotifyUserRegistrationProducerPort {

    private final SqsPublisher sqsPublisher;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public SQSNotifyUserRegistrationProducerAdapter(
            final SqsPublisher sqsPublisher,
            final ObjectMapper objectMapper,
            @Value("${aws.sqs.user-registration-queue-name}") final String queueName) {
        this.sqsPublisher = sqsPublisher;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    @Override
    @SneakyThrows
    public Void execute(final Context context) {
        final var message = context.getData(NotifyUserRegistrationMessage.class);
        sqsPublisher.publish(queueName, objectMapper.writeValueAsString(message));

        return null;
    }
}
