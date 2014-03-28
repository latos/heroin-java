// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

import java.util.concurrent.Callable;

/**
 * Same as {@link Callable} except does not throw Exception.
 *
 * @author dan
 */
public interface Returner<T> extends Callable<T> {

  @Override public T call();
}
