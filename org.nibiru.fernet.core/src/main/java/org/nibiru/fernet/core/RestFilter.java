package org.nibiru.fernet.core;

import com.google.common.io.CharStreams;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class RestFilter implements Filter {
    private final MethodResolver methodResolver;
    private final ServiceProvider serviceProvider;
    private final Map<String, Serializer> serializers;

    @Inject
    public RestFilter(MethodResolver methodResolver,
                      ServiceProvider serviceProvider,
                      Map<String, Serializer> serializers) {
        this.methodResolver = requireNonNull(methodResolver);
        this.serviceProvider = requireNonNull(serviceProvider);
        this.serializers = requireNonNull(serializers);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse resp = (HttpServletResponse) servletResponse;

            HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
            String path = req.getRequestURI().substring(
                    req.getContextPath().length());

            Method method = methodResolver.resolveMethod(httpMethod, path);
            if (method != null) {
                Object[] args = parseStringArgs(
                        getStringArgs(req, path, method), req, method);

                Object response = execute(method, args);

                writeResponse(response, method, req, resp);
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new ServletException(e);
        }
    }

    private String[] getStringArgs(HttpServletRequest req, String path,
                                   Method method) throws IOException {
        String body = CharStreams.toString(new InputStreamReader(req
                .getInputStream()));
        String[] stringArgs = methodResolver.resolveParameters(method, path,
                req.getParameterMap(), body);
        checkState(
                stringArgs.length == method.getParameterTypes().length,
                String.format(
                        "Resolved argument count does not match: %s. Expected: %s",
                        stringArgs.length, method.getParameterTypes().length));
        return stringArgs;
    }

    private Object[] parseStringArgs(String[] stringArgs,
                                     HttpServletRequest req, Method method) {
        String mimeType = methodResolver
                .resolveRequestMimeType(method, req);
        Serializer serializer = serializers.get(mimeType);
        checkState(serializer != null, String.format(
                "Request serializer for MIME type %s not found.", mimeType));
        Object[] args = new Object[stringArgs.length];

        for (int n = 0; n < stringArgs.length; n++) {
            args[n] = serializer.fromString(stringArgs[n],
                    method.getParameterTypes()[n]);
        }
        return args;
    }

    private Object execute(Method method, Object[] args)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        return method.invoke(
                serviceProvider.getService(method.getDeclaringClass()), args);
    }

    private void writeResponse(Object response, Method method,
                               HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String mimeType = methodResolver.resolveRequestMimeType(method, req);
        Serializer serializer = serializers.get(mimeType);
        checkState(serializer != null, String.format(
                "Response serializer for MIME type %s not found.", mimeType));
        resp.setContentType(mimeType);
        resp.getWriter().write(response != null
                ? serializer.toString(response)
                : "");
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}