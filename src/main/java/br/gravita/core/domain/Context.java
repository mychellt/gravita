package br.gravita.core.domain;

import br.gravita.core.domain.system.UserId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;

import static java.util.Optional.ofNullable;

public class Context extends HashMap<String, Object> implements Serializable {
    @Getter
    @AllArgsConstructor
    private enum Parameters {
        DATA("data"), RESUlT("result"), CALLER("caller");
        private final String value;
    }

    public Context(final Object data) {
        super.put(Parameters.DATA.value, data);
    }

    public Context() {
        super();
    }

    /** The authenticated user behind the request, resolved from the session by the inbound adapter. */
    public Context withCaller(final UserId callerId) {
        put(Parameters.CALLER.value, callerId);
        return this;
    }

    public UserId getCaller() {
        return getProperty(Parameters.CALLER.value, UserId.class);
    }

    public Class<?> getDataClass() {
        return ofNullable(get(Parameters.DATA.value))
                .map(Object::getClass)
                .orElse(null);
    }

    public Class<?> getResultClass() {
        return ofNullable(get(Parameters.RESUlT.value))
                .map(Object::getClass)
                .orElse(null);
    }

    public <T> T getData(final Class<T> clazz) {
        return getProperty(Parameters.DATA.value, clazz);
    }

    public void setData(final Object data) {
        put(Parameters.DATA.value, data);
    }

    public <T> T getResult(final Class<T> clazz) {
        return getProperty(Parameters.RESUlT.value, clazz);
    }

    public void setResult(final Object result) {
        put(Parameters.RESUlT.value, result);
    }

    public void putProperty(final String key, final Object value) {
        put(key, value);
    }

    public <R> R getProperty(final String key, final Class<R> clazz) {
        return ofNullable(get(key))
                .map(clazz::cast)
                .orElse(null);
    }
}
