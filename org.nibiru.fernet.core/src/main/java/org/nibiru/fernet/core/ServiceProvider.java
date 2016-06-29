package org.nibiru.fernet.core;

public interface ServiceProvider {
	<T> T getService(Class<T> serviceClass);
}
