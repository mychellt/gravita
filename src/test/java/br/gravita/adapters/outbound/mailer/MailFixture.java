package br.gravita.adapters.outbound.mailer;

import br.gravita.core.domain.mailer.Mail;
import br.gravita.core.domain.mailer.MailType;
import net.datafaker.Faker;

final class MailFixture {

    private MailFixture() {
    }

    static Mail createValid() {
        final var faker = new Faker();
        return Mail.builder()
                .type(MailType.TEXT)
                .replyTo(faker.internet().emailAddress())
                .from(faker.internet().emailAddress())
                .subject(faker.lorem().sentence())
                .recipient(faker.internet().emailAddress())
                .content(faker.lorem().paragraph())
                .build();
    }
}
