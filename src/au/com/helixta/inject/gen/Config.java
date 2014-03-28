// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 *
 *
 * @author dan
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Config {

    /** The config param name */
    String value() default "";
}

