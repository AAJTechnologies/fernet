package org.nibiru.fernet.guice;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RUNTIME)
public @interface Service {
}
