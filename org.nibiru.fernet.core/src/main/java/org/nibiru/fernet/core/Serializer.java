package org.nibiru.fernet.core;

public interface Serializer {
	<T> T fromString(String data, Class<T> type);

	String toString(Object data);
}
