package br.gravita.adapters.inbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import br.gravita.core.usercases.system.SendPasswordResetEmailUseCase;
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
@ConditionalOnProperty(name = "aws.sqs.password-reset-consumer.enabled", havingValue = "true", matchIfMissing = true)
public class SQSNotifyPasswordResetConsumer {

    private final ObjectMapper objectMapper;
    private final SendPasswordResetEmailUseCase sendPasswordResetEmailUseCase;

    @SqsListener("${aws.sqs.password-reset-queue-name}")
    public void onMessage(final String body) {
        final NotifyPasswordResetMessage reset;
        try {
            reset = objectMapper.readValue(body, NotifyPasswordResetMessage.class);
        } catch (final JacksonException e) {
            log.error("Discarding malformed password reset message");
            return;
        }
        if (!isComplete(reset)) {
            log.error("Discarding password reset message: it lacks the recipient, name or reset token");
            return;
        }
        sendPasswordResetEmailUseCase.execute(new Context(reset));
    }

    private static boolean isComplete(final NotifyPasswordResetMessage message) {
        return hasText(message.recipient()) && hasText(message.username()) && hasText(message.token());
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
