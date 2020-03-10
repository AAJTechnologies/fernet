package org.nibiru.fernet.core.executor;

import org.nibiru.fernet.core.MethodExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class CompletableFutureMethodExecutor implements MethodExecutor {
    @Override
    public boolean canHandle(Method method) {
        return method.getReturnType()
                .isAssignableFrom(CompletableFuture.class);
    }

    @Override
    public CompletableFuture<?> execute(Object instance,
                                        Method method,
                                        Object[] args) {
        requireNonNull(instance);
        requireNonNull(method);
        requireNonNull(args);
        try {
            return (CompletableFuture<?>) method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
