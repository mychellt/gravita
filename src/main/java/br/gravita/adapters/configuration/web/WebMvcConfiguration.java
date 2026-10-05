package br.gravita.adapters.configuration.web;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUserArgumentResolver;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

	private final ObjectProvider<SessionStorePort> sessionStorePortProvider;

	public WebMvcConfiguration(final ObjectProvider<SessionStorePort> sessionStorePortProvider) {
		this.sessionStorePortProvider = sessionStorePortProvider;
	}

	@Override
	public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new AuthenticatedUserArgumentResolver(sessionStorePortProvider));
	}
}
