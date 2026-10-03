package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.ActivationEmailRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Slf4j
@RequiredArgsConstructor
@Component
public class ActivationEmailRequestedPublisher implements Command<Void> {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Void execute(final Context context) {
        ofNullable(context.getData(ActivationEmailRequested.class))
                .ifPresentOrElse(eventPublisher::publishEvent,
                        () -> log.warn("No activation email requested data found"));
        return null;
    }
}
