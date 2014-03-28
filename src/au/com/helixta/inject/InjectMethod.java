// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
 * Marks a method to have an injector generated for it.
 *
 * <p>
 * The injector will implement the single-method interface given as an
 * annotation argument by passing through its arguments to the inner method, and
 * sourcing additional arguments from the current scope.
 *
 * <p>
 * NOTE(dan): This breaks proper practices somewhat, because the injected method
 * declares how it is to be injected. On the other hand, it is syntactically
 * convenient, and there is no clearly nice, method-rename-robust way to
 * externally configure methods for injection. Perhaps with Java 8 there will be
 * nicer ways. Similar but less severe caveats apply to using
 * {@link javax.inject.Inject} on methods.
 *
 * @author dan
 */
@Retention(RUNTIME)
public @interface InjectMethod {
  Class<?> value();
}
