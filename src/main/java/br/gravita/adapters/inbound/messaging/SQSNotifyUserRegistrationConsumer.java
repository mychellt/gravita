package br.gravita.adapters.inbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.usercases.system.SendUserRegistrationEmailUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.sqs.user-registration-consumer.enabled", havingValue = "true", matchIfMissing = true)
public class SQSNotifyUserRegistrationConsumer {

    private final ObjectMapper objectMapper;
    private final SendUserRegistrationEmailUseCase sendUserRegistrationEmailUseCase;

    @SqsListener("${aws.sqs.user-registration-queue-name}")
    public void onMessage(final String body) {
        final NotifyUserRegistrationMessage registration;
        try {
            registration = objectMapper.readValue(body, NotifyUserRegistrationMessage.class);
        } catch (JacksonException e) {
            log.error("Discarding malformed user registration message");
            return;
        }
        if (!isComplete(registration)) {
            log.error("Discarding user registration message: it lacks the recipient, name or activation token");
            return;
        }
        sendUserRegistrationEmailUseCase.execute(new Context(registration));
    }

    private static boolean isComplete(final NotifyUserRegistrationMessage message) {
        return hasText(message.recipient()) && hasText(message.username()) && hasText(message.token());
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
