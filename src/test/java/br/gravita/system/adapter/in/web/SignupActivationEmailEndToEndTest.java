package br.gravita.system.adapter.in.web;

import br.gravita.adapters.configuration.async.AsyncConfiguration;
import br.gravita.core.ports.messaging.SendActivationEmailPort;
import br.gravita.core.ports.messaging.records.ActivationEmailRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole feature through the real application: {@code POST /api/signup} commits, the after-commit async listener
 * issues the token and "sends" the mail (the mail port is a stub), and {@code GET /api/activate} consumes the
 * mailed token. Deliberately <em>not</em> {@code @Transactional}: a test-managed transaction never commits, so the
 * after-commit listener would never fire. It cleans up the rows it creates instead.
 *
 * <p>Needs the seeded Silver plan and Administrator profile (Flyway migrations V5 and V77).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SignupActivationEmailEndToEndTest {

    private static final long TIMEOUT_SECONDS = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    @Qualifier(AsyncConfiguration.ACTIVATION_EMAIL_EXECUTOR)
    private ThreadPoolTaskExecutor mailExecutor;

    @MockitoBean
    private SendActivationEmailPort sendActivationEmailPort;

    private final List<String> createdEmails = new ArrayList<>();
    private final List<String> createdCnpjs = new ArrayList<>();

    @BeforeEach
    void resetMailStub() {
        reset(sendActivationEmailPort);
    }

    @AfterEach
    void cleanUpCreatedRows() throws Exception {
        awaitMailExecutorIdle();
        for (String email : createdEmails) {
            jdbc.update("delete from activation_tokens where user_id in (select id from users where email = ?)", email);
            jdbc.update("delete from users where email = ?", email);
        }
        for (String cnpj : createdCnpjs) {
            jdbc.update("delete from payments where subscription_id in (select id from subscriptions where person_id in"
                    + " (select id from persons where document = ?))", cnpj);
            jdbc.update("delete from subscriptions where person_id in (select id from persons where document = ?)", cnpj);
            jdbc.update("delete from persons where document = ?", cnpj);
        }
    }

    private String newEmail() {
        String email = "ana-" + UUID.randomUUID() + "@acme.test";
        createdEmails.add(email);
        return email;
    }

    private String newCnpj() {
        int[] digits = new int[14];
        for (int i = 0; i < 12; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }
        digits[0] = 1 + digits[0] % 9; // never an all-zero / all-equal CNPJ
        digits[12] = checkDigit(digits, 12, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        digits[13] = checkDigit(digits, 13, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        StringBuilder cnpj = new StringBuilder();
        for (int digit : digits) {
            cnpj.append(digit);
        }
        createdCnpjs.add(cnpj.toString());
        return cnpj.toString();
    }

    private static int checkDigit(int[] digits, int length, int[] weights) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private ResultActions signUp(String email, String cnpj, String plan) throws Exception {
        return mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Ana Souza","email":"%s","password":"s3cret-pass","companyName":"Acme Ltda",
                 "cnpj":"%s","phone":"(11) 91234-5678","plan":"%s","billing":"monthly"}""".formatted(email, cnpj, plan)));
    }

    private String statusOf(String email) {
        return jdbc.queryForObject("select status from users where email = ?", String.class, email);
    }

    private int countTokens() {
        return jdbc.queryForObject("select count(*) from activation_tokens", Integer.class);
    }

    private void awaitMailExecutorIdle() throws InterruptedException {
        ThreadPoolExecutor pool = mailExecutor.getThreadPoolExecutor();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while ((pool.getActiveCount() > 0 || !pool.getQueue().isEmpty()) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }

    @Test
    @DisplayName("AC1/AC3/AC4/AC5: signup leaves the user pending, mails a token, the token activates, and replay is refused")
    void signupMailActivateAndReplay() throws Exception {
        CompletableFuture<ActivationEmailRequest> mailed = new CompletableFuture<>();
        doAnswer(call -> mailed.complete(call.getArgument(0))).when(sendActivationEmailPort).send(any());
        String email = newEmail();

        signUp(email, newCnpj(), "silver").andExpect(status().isCreated());

        ActivationEmailRequest mail = mailed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(mail.to()).isEqualTo(email);
        assertThat(mail.recipientName()).isEqualTo("Ana Souza");
        assertThat(statusOf(email)).isEqualTo("PENDING_ACTIVATION");

        mockMvc.perform(get("/api/activate").param("token", mail.activationToken())).andExpect(status().isOk());
        assertThat(statusOf(email)).isEqualTo("ACTIVE");

        mockMvc.perform(get("/api/activate").param("token", mail.activationToken())).andExpect(status().isGone());
        assertThat(statusOf(email)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("AC2: a slow mail port does not delay the signup's 201")
    void aSlowMailPortDoesNotDelayTheSignupResponse() throws Exception {
        CountDownLatch mailStarted = new CountDownLatch(1);
        CountDownLatch releaseMail = new CountDownLatch(1);
        CountDownLatch mailFinished = new CountDownLatch(1);
        doAnswer(call -> {
            mailStarted.countDown();
            releaseMail.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            mailFinished.countDown();
            return null;
        }).when(sendActivationEmailPort).send(any());

        signUp(newEmail(), newCnpj(), "silver").andExpect(status().isCreated());

        assertThat(mailStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(mailFinished.getCount()).as("the 201 came back while the mail was still being sent").isEqualTo(1);
        releaseMail.countDown();
        assertThat(mailFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("AC2: a throwing mail port does not fail the signup; the account is still created")
    void aThrowingMailPortDoesNotFailTheSignup() throws Exception {
        doThrow(new IllegalStateException("mail queue unreachable")).when(sendActivationEmailPort).send(any());
        String email = newEmail();

        signUp(email, newCnpj(), "silver").andExpect(status().isCreated());

        verify(sendActivationEmailPort, timeout(TIMEOUT_SECONDS * 1000)).send(any());
        assertThat(statusOf(email)).isEqualTo("PENDING_ACTIVATION");
    }

    @Test
    @DisplayName("AC7: rolled-back signups (duplicate e-mail, duplicate CNPJ, unknown plan) issue no token and send no mail")
    void rolledBackSignupsIssueNoTokenAndSendNoMail() throws Exception {
        String email = newEmail();
        String cnpj = newCnpj();
        signUp(email, cnpj, "silver").andExpect(status().isCreated());
        verify(sendActivationEmailPort, timeout(TIMEOUT_SECONDS * 1000)).send(any());
        awaitMailExecutorIdle();
        int tokensAfterFirstSignup = countTokens();

        signUp(email, newCnpj(), "silver").andExpect(status().isBadRequest());
        signUp(newEmail(), cnpj, "silver").andExpect(status().isBadRequest());
        signUp(newEmail(), newCnpj(), "platinum").andExpect(status().isBadRequest());
        awaitMailExecutorIdle();

        verify(sendActivationEmailPort, times(1)).send(any());
        assertThat(countTokens()).isEqualTo(tokensAfterFirstSignup);
    }
}
