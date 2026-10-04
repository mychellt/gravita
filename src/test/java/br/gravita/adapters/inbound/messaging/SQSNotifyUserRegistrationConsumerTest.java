package br.gravita.adapters.inbound.messaging;

import br.gravita.core.domain.Context;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.usercases.system.SendUserRegistrationEmailUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SQSNotifyUserRegistrationConsumerTest {

    @Mock
    private SendUserRegistrationEmailUseCase sendUserRegistrationEmailUseCase;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private SQSNotifyUserRegistrationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SQSNotifyUserRegistrationConsumer(objectMapper, sendUserRegistrationEmailUseCase);
    }

    private String registrationJson() {
        return objectMapper.writeValueAsString(NotifyUserRegistrationMessage.builder()
                .username("Ana Souza").recipient("ana@acme.com").token("tok").tenantId(UUID.randomUUID()).build());
    }

    @Test
    @DisplayName("A registration message is turned into the activation e-mail")
    void shouldSendTheEmail() {
        consumer.onMessage(registrationJson());

        final var context = ArgumentCaptor.forClass(Context.class);
        verify(sendUserRegistrationEmailUseCase).execute(context.capture());
        final var message = context.getValue().getData(NotifyUserRegistrationMessage.class);
        assertThat(message.recipient()).isEqualTo("ana@acme.com");
        assertThat(message.username()).isEqualTo("Ana Souza");
        assertThat(message.token()).isEqualTo("tok");
    }

    @Test
    @DisplayName("A failed e-mail propagates, so the message is not acknowledged and SQS redelivers it")
    void shouldPropagateASendFailure() {
        doThrow(new IllegalStateException("smtp down")).when(sendUserRegistrationEmailUseCase).execute(any());

        assertThatThrownBy(() -> consumer.onMessage(registrationJson())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A malformed message returns normally (so it is acknowledged and dropped) without sending anything")
    void shouldDiscardAMalformedMessage() {
        consumer.onMessage("{not json");

        verify(sendUserRegistrationEmailUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("A message without an activation token can never be mailed, so it is dropped instead of retried")
    void shouldDiscardAnIncompleteMessage() {
        consumer.onMessage("{\"username\":\"Ana\",\"recipient\":\"ana@acme.com\",\"token\":null}");

        verify(sendUserRegistrationEmailUseCase, never()).execute(any());
    }
}
