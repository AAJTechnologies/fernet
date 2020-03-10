package org.nibiru.fernet.core;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface MethodExecutor {
    boolean canHandle(Method method);

    CompletableFuture<?> execute(Object instance,
                                 Method method,
                                 Object[] args);
}
