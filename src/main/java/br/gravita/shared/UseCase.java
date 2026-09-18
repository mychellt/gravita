package br.gravita.shared;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an inbound use-case implementation (application.service layer). A thin
 * alias over {@link Component} so the application layer reads in domain
 * vocabulary instead of Spring's.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface UseCase {
}
