package br.gravita.core.ports.persistence.commons;

public interface WriteRepositoryPort<T> {
    T save(final T model);
}
