package org.nibiru.fernet.core;

import javax.annotation.Nullable;

public interface Serializer {
	@Nullable
	<T> T fromString(@Nullable String data, Class<T> type);

	@Nullable
	String toString(@Nullable Object data);
}
