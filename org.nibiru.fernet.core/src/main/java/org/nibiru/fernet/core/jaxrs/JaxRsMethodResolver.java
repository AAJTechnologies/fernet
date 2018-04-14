package org.nibiru.fernet.core.jaxrs;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.nibiru.fernet.core.HttpMethod;
import org.nibiru.fernet.core.MethodResolver;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class JaxRsMethodResolver implements MethodResolver {
    private static final Iterable<Class<? extends Annotation>> HTTP_METHODS_ANNOTATIONS
            = ImmutableList.of(GET.class, POST.class, PUT.class, DELETE.class, HEAD.class);
    private final String defaultContentType;
    private final BiMap<MethodDefinition, Method> methodDefinitions;

    public JaxRsMethodResolver(String defaultContentType,
                               Class<?>... serviceClasses) {
        this.defaultContentType = requireNonNull(defaultContentType);
        requireNonNull(serviceClasses);
        methodDefinitions = HashBiMap.create();
        for (Class<?> serviceClass : serviceClasses) {
            for (Method method : serviceClass.getMethods()) {
                Path path = method.getAnnotation(Path.class);
                Annotation httpMethod = firstAnnotation(method, HTTP_METHODS_ANNOTATIONS);
                if (path != null && httpMethod != null) {
                    MethodDefinition methodDefinition =
                            new MethodDefinition(httpMethodFromAnnotation(httpMethod),
                                    Pattern.compile(pathToRegex(path.value())));

                    Method existingMethod = methodDefinitions.get(methodDefinition);
                    if (existingMethod == null || isGenericOverride(method, existingMethod)) {
                        // TODO: check the @PathParam values to early detect errors
                        methodDefinitions
                                .put(methodDefinition, method);
                    }
                }
            }
        }
    }

    @Override
    public Method resolveMethod(HttpMethod httpMethod,
                                String path) {
        requireNonNull(httpMethod);
        requireNonNull(path);
        MethodDefinition methodDefinition = Iterables.find(
                methodDefinitions.keySet(), methodDefinition1 ->
                        methodDefinition1.httpMethod == httpMethod
                                && methodDefinition1.pathPattern.matcher(path)
                                .matches(), null);
        return methodDefinition != null
                ? methodDefinitions.get(methodDefinition)
                : null;
    }

    @Override
    public String[] resolveParameters(Method method, String path,
                                      Map<String, String[]> reqParams,
                                      String body) {
        String[] values = new String[method.getParameterTypes().length];
        for (int n = 0; n < values.length; n++) {
            Annotation[] annotations = method.getParameterAnnotations()[n];

            QueryParam queryParam = findAnnotation(QueryParam.class,
                    annotations);
            if (queryParam != null) {
                String[] valueArray = reqParams.get(queryParam.value());
                if (valueArray != null) {
                    // TODO: Which solution should be implemented for repeated
                    // parameters? Taking first value for now.
                    values[n] = valueArray[0];
                } else {
                    DefaultValue defaultValue = findAnnotation(
                            DefaultValue.class, annotations);
                    values[n] = defaultValue != null ? defaultValue.value()
                            : "";
                }
            } else {
                PathParam pathParan = findAnnotation(PathParam.class,
                        annotations);
                if (pathParan != null) {
                    String pathParamStr = "{" + pathParan.value() + "}";
                    values[n] = "";
                    Pattern pathPattern = methodDefinitions.inverse()
                            .get(method)
                            .pathPattern;
                    Matcher annotationMatcher = pathPattern.matcher(method
                            .getAnnotation(Path.class).value());
                    Matcher requestMatcher = pathPattern.matcher(path);
                    annotationMatcher.find();
                    requestMatcher.find();
                    for (int g = 1; g <= annotationMatcher.groupCount(); g++) {
                        if (annotationMatcher.group(g).equals(pathParamStr)) {
                            values[n] = requestMatcher.group(g);
                        }
                    }
                } else {
                    values[n] = body;
                }
            }
        }
        return values;
    }

    @Override
    public String resolveRequestMimeType(Method method,
                                         HttpServletRequest request) {
        // TODO Read @Consumes annotation
        return parseMimeType(request.getContentType());
    }

    @Override
    public String resolveResponseMimeType(Method method,
                                          HttpServletRequest request) {
        // TODO Read @Produces annotation
        return parseMimeType(request.getHeader("Accept"));
    }

    private String parseMimeType(String contentType) {
        if (contentType != null) {
            int pos = contentType.indexOf(';');
            return pos <= 0 ? contentType : contentType.substring(0, pos)
                    .trim();
        } else {
            return defaultContentType;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T findAnnotation(Class<T> annotationClass,
                                                    Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotationClass.isAssignableFrom(annotation.getClass())) {
                return (T) annotation;
            }
        }
        return null;
    }

    private String pathToRegex(String path) {
        return "^" + path.replaceAll("\\{.+?\\}", "(.+?)") + "$";
    }

    private Annotation firstAnnotation(Method method,
                                       Iterable<Class<? extends Annotation>> annotationClasses) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private HttpMethod httpMethodFromAnnotation(Annotation annotation) {
        if (annotation instanceof POST) {
            return HttpMethod.POST;
        } else if (annotation instanceof PUT) {
            return HttpMethod.PUT;
        } else if (annotation instanceof DELETE) {
            return HttpMethod.DELETE;
        } else if (annotation instanceof HEAD) {
            return HttpMethod.HEAD;
        } else {
            return HttpMethod.GET;
        }
    }

    private boolean isGenericOverride(Method method,
                                      Method existingMethod) {
        checkArgument(method.getName().equals(existingMethod.getName())
                        && method.getParameterTypes().length == existingMethod.getParameterTypes().length,
                "Trying to compare different methods: %s and %s",
                method,
                existingMethod);

        // Overriden methods with parameterized types have "Object" as parameter type.
        return existingMethod.getGenericParameterTypes().length > 0
                && existingMethod.getGenericParameterTypes()[0].equals(Object.class);
    }

    private static class MethodDefinition {
        private final HttpMethod httpMethod;
        private final Pattern pathPattern;

        private MethodDefinition(HttpMethod httpMethod,
                                 Pattern pathPattern) {
            this.httpMethod = httpMethod;
            this.pathPattern = pathPattern;
        }

        @Override
        public int hashCode() {
            // Pattern seems to don not have hashcode implemented!
            return Objects.hash(httpMethod, pathPattern.pattern());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MethodDefinition other = (MethodDefinition) obj;
            return Objects.equals(httpMethod, other.httpMethod)
                    && Objects.equals(pathPattern.pattern(), other.pathPattern.pattern());
        }
    }
}
