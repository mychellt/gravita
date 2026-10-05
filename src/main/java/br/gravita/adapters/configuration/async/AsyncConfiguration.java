package br.gravita.adapters.configuration.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration {

	public static final String ACTIVATION_EMAIL_EXECUTOR = "activationEmailExecutor";

	/**
	 * A saturated queue must not fail the signup request: the listener is submitted from the signup's after-commit
	 * callback, where a rejection would surface as a 500 even though the account was created. Dropping (and logging)
	 * is the lesser evil; the user can ask for a new link.
	 */
	@Bean(name = ACTIVATION_EMAIL_EXECUTOR)
	public Executor activationEmailExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("activation-email-");
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(200);
		executor.setRejectedExecutionHandler((task, pool) ->
				log.error("Activation e-mail dropped: executor saturated (queue full)"));
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		return executor;
	}
}
