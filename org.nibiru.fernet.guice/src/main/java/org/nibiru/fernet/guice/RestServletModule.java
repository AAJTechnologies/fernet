package org.nibiru.fernet.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.nibiru.fernet.core.MethodResolver;
import org.nibiru.fernet.core.jaxrs.JaxRsMethodResolver;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class RestServletModule extends AbstractModule {
    private final String defaultContentType;
    private Class<?>[] serviceClasses;

    public RestServletModule(@Nonnull String defaultContentType,
							 @Nonnull Class<?>... serviceClasses) {
        this.defaultContentType = requireNonNull(defaultContentType);
        this.serviceClasses = requireNonNull(serviceClasses);
    }

    @Override
    protected void configure() {
        install(new CommonModule());
    }

    @Provides
    @Singleton
    public MethodResolver getMethodResolver() {
        return new JaxRsMethodResolver(defaultContentType, serviceClasses);
    }
}