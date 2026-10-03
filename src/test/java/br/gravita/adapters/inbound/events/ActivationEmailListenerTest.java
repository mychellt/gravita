package br.gravita.adapters.inbound.events;

import br.gravita.adapters.configuration.async.AsyncConfiguration;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserSignedUp;
import br.gravita.core.usercases.system.SendActivationEmailCommand;
import br.gravita.core.usercases.system.SendActivationEmailUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Wires the real listener, the real {@code @Async} executor and Spring's real transaction-synchronization machinery
 * (over a no-op transaction manager), with only the e-mail use case stubbed. That is what proves the timing and
 * isolation claims, rather than just that the annotations are present.
 */
class ActivationEmailListenerTest {

	private static final long TIMEOUT_MS = 5_000;

	private AnnotationConfigApplicationContext context;
	private SendActivationEmailUseCase sendActivationEmail;
	private TransactionTemplate transaction;
	private final UserSignedUp event = new UserSignedUp(UserId.generate(), "Ana Souza", "ana@acme.com");

	@Configuration
	@EnableTransactionManagement
	@Import({AsyncConfiguration.class, ActivationEmailListener.class})
	static class TestConfig {

		@Bean
		SendActivationEmailUseCase sendActivationEmailUseCase() {
			return Mockito.mock(SendActivationEmailUseCase.class);
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new NoOpTransactionManager();
		}
	}

	static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
		}
	}

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		sendActivationEmail = context.getBean(SendActivationEmailUseCase.class);
		transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	private void signUpInTransaction(boolean commit) {
		transaction.executeWithoutResult(status -> {
			context.publishEvent(event);
			if (!commit) {
				status.setRollbackOnly();
			}
		});
	}

	@Test
	@DisplayName("A slow mail send runs on its own thread and never holds up the signup's commit")
	void commitReturnsWhileTheMailIsStillSending() throws Exception {
		CountDownLatch mailStarted = new CountDownLatch(1);
		CountDownLatch releaseMail = new CountDownLatch(1);
		CountDownLatch mailFinished = new CountDownLatch(1);
		AtomicReference<String> mailThread = new AtomicReference<>();
		doAnswer(call -> {
			mailThread.set(Thread.currentThread().getName());
			mailStarted.countDown();
			releaseMail.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
			mailFinished.countDown();
			return null;
		}).when(sendActivationEmail).execute(any());

		signUpInTransaction(true);

		assertThat(mailStarted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(mailFinished.getCount()).as("the commit returned while the mail was still being sent").isEqualTo(1);
		assertThat(mailThread.get()).startsWith("activation-email-").isNotEqualTo(Thread.currentThread().getName());

		releaseMail.countDown();
		assertThat(mailFinished.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
		verify(sendActivationEmail).execute(
				new SendActivationEmailCommand(event.userId(), "Ana Souza", "ana@acme.com"));
	}

	@Test
	@DisplayName("A failing mail send is swallowed: the signup still commits and returns normally")
	void aMailFailureNeverSurfacesToTheSignup() {
		doThrow(new IllegalStateException("queue unreachable")).when(sendActivationEmail).execute(any());

		assertThatCode(() -> signUpInTransaction(true)).doesNotThrowAnyException();

		verify(sendActivationEmail, timeout(TIMEOUT_MS)).execute(any());
	}

	@Test
	@DisplayName("A rolled-back signup never triggers the e-mail")
	void aRolledBackSignupSendsNothing() {
		signUpInTransaction(false);

		context.close(); // waits for any queued async work, so a wrongly dispatched send would have run by now
		verifyNoInteractions(sendActivationEmail);
	}

	@Test
	@DisplayName("Nothing is sent before the signup commits")
	void nothingIsSentBeforeCommit() {
		transaction.executeWithoutResult(status -> {
			context.publishEvent(event);
			verifyNoInteractions(sendActivationEmail);
		});

		verify(sendActivationEmail, timeout(TIMEOUT_MS)).execute(any());
	}
}
