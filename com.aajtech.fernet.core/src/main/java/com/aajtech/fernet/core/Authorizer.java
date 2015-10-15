package com.aajtech.fernet.core;

import javax.servlet.http.HttpServletRequest;

public interface Authorizer {
	boolean isAuthorized(HttpServletRequest req);
}
