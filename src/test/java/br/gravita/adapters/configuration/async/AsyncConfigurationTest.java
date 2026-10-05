package br.gravita.adapters.configuration.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThatCode;

class AsyncConfigurationTest {

	@Test
	@DisplayName("A saturated activation-email executor drops the task instead of throwing into the signup request")
	void saturationDoesNotThrow() {
		final ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfiguration().activationEmailExecutor();
		executor.afterPropertiesSet();
		final CountDownLatch release = new CountDownLatch(1);
		try {
			final int capacity = executor.getMaxPoolSize() + executor.getQueueCapacity();

			assertThatCode(() -> {
				for (int i = 0; i < capacity + 10; i++) {
					executor.execute(() -> {
						try {
							release.await();
						} catch (final InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});
				}
			}).doesNotThrowAnyException();
		} finally {
			release.countDown();
			executor.shutdown();
		}
	}
}
