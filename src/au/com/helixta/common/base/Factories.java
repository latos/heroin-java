// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

/**
 * Collection of Factory interfaces.
 *
 * Used as a callable with the intended use of creating a new instance each time
 * (as opposed to a Provider, which is mostly used for providing a particular
 * instance, e.g. for lazy construction or dep-cycle breaking)
 *
 * The zero-arg version is still suffixed with a number, to make it much easier
 * to find with IDE support, and to avoid clashing with class-specific Factory
 * inner interfaces, which are a common pattern.
 *
 * @author dan
 */
public class Factories {
  private Factories(){}

  /** @see Factories */
  public interface Factory0<T> {
    /** Create a new instance */
    T create();
  }

  /** @see Factories */
  public interface Factory1<T, A> {
    /** Create a new instance */
    T create(A argA);
  }

  /** @see Factories */
  public interface Factory2<T, A, B> {
    /** Create a new instance */
    T create(A argA, B argB);
  }
}
