package org.nibiru.fernet.core.executor;

import org.nibiru.fernet.core.MethodExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class BlockingMethodExecutor implements MethodExecutor {
    @Override
    public boolean canHandle(Method method) {
        return true;
    }

    @Override
    public CompletableFuture<?> execute(Object instance,
                                        Method method,
                                        Object[] args) {
        requireNonNull(instance);
        requireNonNull(method);
        requireNonNull(args);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
