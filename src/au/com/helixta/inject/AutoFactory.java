// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import au.com.helixta.inject.gen.ScopeBuilder;

import javax.inject.Qualifier;

import java.lang.annotation.Retention;

/**
 * Mark a factory dependency as one to be auto-generated based on the
 * constructor.
 *
 * <p>
 * Currently, auto-factories are the only dep that will be implicitly generated
 * (well, still needs this annotation in the input parameter, but not explicit
 * in the scope building). This is OK because autogenerated factories are simply
 * constructors with some arguments curried and so are inherently stateless;
 * thus it doesn't matter that we are not being explicit about which scope their
 * life cycle should be tied to - their "life cycle" is irrelevant.
 *
 * <p>
 * Note that this annotation is a Qualifier. Thus generated implementations will
 * not be accessible unless explicitly requested.
 *
 * <p>
 * It is not recommended to request auto-factories in injected code, as this
 * leaks the implementation. It should be used mainly for arguments to provider
 * methods. To generate factories visible without the use of the annotation, be
 * explicit with {@link ScopeBuilder#factory(java.lang.reflect.Type, Class...)}
 *
 * @author dan
 */
@Retention(RUNTIME)
@Qualifier
public @interface AutoFactory {
}
