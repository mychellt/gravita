package br.gravita.system.adapter.in.web;

import br.gravita.adapters.configuration.async.AsyncConfiguration;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.IssuedActivationToken;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.messaging.SendActivationEmailPort;
import br.gravita.core.ports.messaging.records.ActivationEmailRequest;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/activate/resend} through the real application, ACs 7-9. Not {@code @Transactional}: the mail is
 * sent by an after-commit async listener, which a never-committing test transaction would not trigger. It deletes the
 * rows it creates instead. The mail port is a stub.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ResendActivationEndToEndTest {

    private static final long TIMEOUT_MS = 10_000;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserRepositoryPort userRepositoryPort;
    @Autowired
    private ProfileRepositoryPort profileRepositoryPort;
    @Autowired
    private ActivationTokenRepositoryPort tokenRepositoryPort;
    @Autowired
    @Qualifier(AsyncConfiguration.ACTIVATION_EMAIL_EXECUTOR)
    private ThreadPoolTaskExecutor mailExecutor;

    @MockitoBean
    private SendActivationEmailPort sendActivationEmailPort;

    private final List<String> createdEmails = new ArrayList<>();
    private IssuedActivationToken signupToken;

    @BeforeEach
    void resetMailStub() {
        reset(sendActivationEmailPort);
    }

    @AfterEach
    void cleanUp() throws Exception {
        ThreadPoolExecutor pool = mailExecutor.getThreadPoolExecutor();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        while ((pool.getActiveCount() > 0 || !pool.getQueue().isEmpty()) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        for (String email : createdEmails) {
            jdbc.update("delete from activation_tokens where user_id in (select id from users where email = ?)", email);
            jdbc.update("delete from users where email = ?", email);
        }
    }

    /**
     * A pending user whose signup token was issued {@code issuedSecondsAgo} seconds ago.
     */
    private User pendingUser(long issuedSecondsAgo) {
        ProfileReference administrator = profileRepositoryPort.findByName("Administrator")
                .orElseGet(() -> new ProfileReference(UUID.randomUUID(), "Administrator"));
        String email = "ana-" + UUID.randomUUID() + "@acme.test";
        createdEmails.add(email);
        User user = userRepositoryPort.save(User.signUp("Ana Souza", email, "s3cret-pass", administrator, null));
        signupToken = ActivationToken.issue(user.getId(), Instant.now().minusSeconds(issuedSecondsAgo));
        tokenRepositoryPort.save(signupToken.token());
        // created_at is stamped by the database layer on insert, so age the row explicitly.
        jdbc.update("update activation_tokens set created_at = created_at - make_interval(secs => ?) where user_id = ?",
                issuedSecondsAgo, user.getId().value());
        return user;
    }

    private String resend(String email) throws Exception {
        return mockMvc.perform(post("/api/activate/resend").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
    }

    private ActivationEmailRequest awaitMail() {
        ArgumentCaptor<ActivationEmailRequest> mail = ArgumentCaptor.forClass(ActivationEmailRequest.class);
        verify(sendActivationEmailPort, timeout(TIMEOUT_MS)).send(mail.capture());
        return mail.getValue();
    }

    @Test
    @DisplayName("AC7: a resend mails a new token, the old link stops working and the new one activates")
    void resendIssuesANewTokenAndInvalidatesTheOldOne() throws Exception {
        User user = pendingUser(120);
        String oldHash = signupToken.token().getTokenHash();

        resend(user.getEmail());

        ActivationEmailRequest mail = awaitMail();
        assertThat(mail.to()).isEqualTo(user.getEmail());
        assertThat(ActivationToken.hash(mail.activationToken())).isNotEqualTo(oldHash);
        assertThat(jdbc.queryForObject("select count(*) from activation_tokens where user_id = ?", Integer.class,
                user.getId().value())).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(*) from activation_tokens where user_id = ? and used_at is null and expires_at > now()",
                Integer.class, user.getId().value())).as("only the new token is still open").isEqualTo(1);

        mockMvc.perform(get("/api/activate").param("token", mail.activationToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC7: the previous link is refused as expired once a resend replaced it")
    void thePreviousLinkIsRefusedAfterAResend() throws Exception {
        User user = pendingUser(120);
        String previousLink = signupToken.rawToken();

        resend(user.getEmail());
        awaitMail();

        mockMvc.perform(get("/api/activate").param("token", previousLink)).andExpect(status().isGone());
    }

    @Test
    @DisplayName("AC8: unknown and already active e-mails get the identical response as a real resend, and no mail goes out")
    void unknownAndActiveEmailsAreIndistinguishableAndSendNothing() throws Exception {
        User pending = pendingUser(120);
        String success = resend(pending.getEmail());
        awaitMail();
        reset(sendActivationEmailPort);

        User active = pendingUser(120);
        active.activate();
        userRepositoryPort.update(active);

        String unknown = resend("ghost-" + UUID.randomUUID() + "@acme.test");
        String activeUser = resend(active.getEmail());

        assertThat(unknown).isEqualTo(success);
        assertThat(activeUser).isEqualTo(success);
        verify(sendActivationEmailPort, after(500).never()).send(any());
    }

    @Test
    @DisplayName("AC9: a second resend inside the cooldown is a no-op: same response, no second mail, no second token")
    void aSecondResendInsideTheCooldownSendsNothing() throws Exception {
        User user = pendingUser(120);

        String first = resend(user.getEmail());
        awaitMail();
        int tokensAfterFirst = jdbc.queryForObject("select count(*) from activation_tokens where user_id = ?",
                Integer.class, user.getId().value());
        reset(sendActivationEmailPort);

        String second = resend(user.getEmail());

        assertThat(second).isEqualTo(first);
        verify(sendActivationEmailPort, after(500).never()).send(any());
        assertThat(jdbc.queryForObject("select count(*) from activation_tokens where user_id = ?", Integer.class,
                user.getId().value())).isEqualTo(tokensAfterFirst);
    }
}
