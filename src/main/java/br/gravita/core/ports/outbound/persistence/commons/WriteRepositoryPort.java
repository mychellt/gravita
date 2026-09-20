package br.gravita.core.ports.outbound.persistence.commons;

public interface WriteRepositoryPort<T> {
    T save(final T model);
}
