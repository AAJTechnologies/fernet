package org.nibiru.fernet.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.nibiru.fernet.core.MethodResolver;
import org.nibiru.fernet.core.jaxrs.JaxRsMethodResolver;

import javax.annotation.Nonnull;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class ExtensibleModule extends AbstractModule {
    private final String defaultContentType;

    public ExtensibleModule(String defaultContentType) {
        this.defaultContentType = requireNonNull(defaultContentType);
    }

    @Override
    protected void configure() {
        install(new CommonModule());
    }

    @Provides
    @Singleton
    public MethodResolver getMethodResolver(@Nonnull @Service Set<Class<?>> serviceClasses) {
        return new JaxRsMethodResolver(this.defaultContentType,
                serviceClasses.toArray(new Class<?>[0]));
    }
}
