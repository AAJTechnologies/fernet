package com.aajtech.fernet.core;

public interface ServiceProvider {
	<T> T getService(Class<T> serviceClass);
}
