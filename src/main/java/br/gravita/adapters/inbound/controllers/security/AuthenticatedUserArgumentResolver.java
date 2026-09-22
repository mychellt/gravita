package br.gravita.adapters.inbound.controllers.security;

import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Depends on an {@link ObjectProvider} rather than {@link SessionStorePort} directly so this
 * resolver - picked up by every {@code @WebMvcTest} slice because it implements {@link
 * HandlerMethodArgumentResolver} - doesn't force a {@code SessionStorePort} bean into test slices
 * for controllers that never use {@link AuthenticatedUser}.
 */
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ObjectProvider<SessionStorePort> sessionStorePortProvider;

	public AuthenticatedUserArgumentResolver(ObjectProvider<SessionStorePort> sessionStorePortProvider) {
		this.sessionStorePortProvider = sessionStorePortProvider;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AuthenticatedUser.class) && parameter.getParameterType() == UserId.class;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String header = webRequest.getHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			throw new UnauthorizedException("Missing or malformed Authorization header");
		}
		String sessionToken = header.substring(BEARER_PREFIX.length()).trim();
		return sessionStorePortProvider.getObject().resolve(sessionToken)
				.orElseThrow(() -> new UnauthorizedException("Invalid or expired session token"));
	}
}
