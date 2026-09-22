package br.gravita.adapters.inbound.controllers.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves a controller method parameter of type {@code UserId} to the caller identified by the
 * request's {@code Authorization: Bearer <sessionToken>} header (see UC-M10-05 / UC-M10-06). Every
 * controller that calls {@code CheckPermissionUseCase} obtains the caller's id this way instead of
 * accepting it from the client as a request parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedUser {
}
