package org.nibiru.fernet.guice;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.servlet.ServletModule;
import org.nibiru.fernet.core.MethodExecutor;
import org.nibiru.fernet.core.RestFilter;
import org.nibiru.fernet.core.Serializer;
import org.nibiru.fernet.core.ServiceProvider;
import org.nibiru.fernet.core.executor.BlockingMethodExecutor;
import org.nibiru.fernet.core.executor.CompletableFutureMethodExecutor;
import org.nibiru.fernet.core.gson.GsonSerializer;

import java.util.List;

public class CommonModule extends ServletModule {
    @Override
    protected void configureServlets() {
        filter("*").through(RestFilter.class);
        bind(RestFilter.class).in(Singleton.class);
        bind(ServiceProvider.class).to(InjectorServiceProvider.class);
        bind(Serializer.class).to(GsonSerializer.class);
        MapBinder<String, Serializer> mapBinder = MapBinder.newMapBinder(
                binder(), String.class, Serializer.class);
        mapBinder.addBinding("application/json").to(GsonSerializer.class);
    }

    @Provides
    @Singleton
    public List<MethodExecutor> getMethodExecutors(CompletableFutureMethodExecutor futureMethodExecutor,
                                                   BlockingMethodExecutor blockingMethodExecutor) {
        return ImmutableList.of(futureMethodExecutor, blockingMethodExecutor);
    }
}
