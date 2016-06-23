package com.aajtech.fernet.guice;

import static java.util.Objects.requireNonNull;

import com.aajtech.fernet.core.MethodResolver;
import com.aajtech.fernet.core.RestFilter;
import com.aajtech.fernet.core.Serializer;
import com.aajtech.fernet.core.ServiceProvider;
import com.aajtech.fernet.core.gson.GsonSerializer;
import com.aajtech.fernet.core.jaxrs.JaxRsMethodResolver;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.servlet.ServletModule;

public class RestServletModule extends ServletModule {
	private final String defaultContentType;
	private Class<?> serviceClasses[];

	public RestServletModule(String defaultContentType,
			Class<?>... serviceClasses) {
		this.defaultContentType = requireNonNull(defaultContentType);
		this.serviceClasses = requireNonNull(serviceClasses);
	}

	@Override
	protected void configureServlets() {
		filter("*").through(RestFilter.class);
		bind(RestFilter.class).in(Singleton.class);
		bind(ServiceProvider.class).to(InjectorServiceProvider.class);
		bind(Serializer.class).to(GsonSerializer.class);
		MapBinder<String, Serializer> mapbinder = MapBinder.newMapBinder(
				binder(), String.class, Serializer.class);
		mapbinder.addBinding("application/json").to(GsonSerializer.class);
	}

	@Provides
	@Singleton
	public MethodResolver getMethodResolver() {
		return new JaxRsMethodResolver(defaultContentType, serviceClasses);
	}
}