package br.gravita.core.domain;

public interface Command<R> {
    R execute(final Context context);
}
