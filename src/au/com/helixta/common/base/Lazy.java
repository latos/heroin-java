// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

/**
 * Provider with semantics of lazy initialization + reuse.
 *
 * Thread safety undefined.
 *
 * @author dan
 */
public interface Lazy<T> {

  /**
   * Create and save the instance if this is the first call
   *
   * @return the saved instance.
   */
  T get();
}
