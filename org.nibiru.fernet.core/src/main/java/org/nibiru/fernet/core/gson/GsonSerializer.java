package org.nibiru.fernet.core.gson;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.nibiru.fernet.core.Serializer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class GsonSerializer implements Serializer {
    private final Gson gson;

    @Inject
    public GsonSerializer(Gson gson) {
        this.gson = requireNonNull(gson);
    }

    @Override
    @Nullable
    public <T> T fromString(String data, Class<T> type) {
        requireNonNull(type);
        if (data != null) {
            if (isPrimitive(type)) {
                if (String.class.isAssignableFrom(type)) {
                    return (T) data;
                } else {
                    return Optional.of(data)
                            .map(Strings::emptyToNull)
                            .map(d -> objectToString(d, type))
                            .orElse(null);
                }
            } else {
                return gson.fromJson(data, type);
            }
        } else {
            return null;
        }
    }

    private <T> T objectToString(String data, Class<T> type) {
        try {
            return type.getConstructor(String.class).newInstance(data);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Nullable
    public String toString(Object data) {
        if (data != null) {
            if (isPrimitive(data.getClass())) {
                return data.toString();
            } else {
                return gson.toJson(data);
            }
        } else {
            return null;
        }
    }

    private boolean isPrimitive(Class<?> type) {
        return String.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Double.class.isAssignableFrom(type);
    }
}
