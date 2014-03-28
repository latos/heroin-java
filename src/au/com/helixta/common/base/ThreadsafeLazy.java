// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Threadsafe lazy initializing implementation.
 *
 * @author dan
 */
public abstract class ThreadsafeLazy<T> implements Lazy<T> {

  /**
   * Override this method to provide the actual object.
   *
   * <p>
   * Guaranteed to be called at most once.
   *
   * @return the created object. Must not be null.
   *
   * @throws Exception
   *           if object could not be created. In this case, this Lazy object
   *           will throw an exception every time {@link #get()} is called.
   *           {@code create()} will never be called again.
   */
  protected abstract T create() throws Exception;

  public ThreadsafeLazy() {
    this("object");
  }

  public ThreadsafeLazy(String name) {
    this.name = checkNotNull(name);
  }

  /*
   * Non-obvious tricks required for correctness. See
   * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
   * http://en.wikipedia.org/wiki/Double-checked_locking
   *
   * TODO(dan): Try other options and profile to find best performant one.
   */
  private static class FinalWrapper<X> {
    private final X object;

    private FinalWrapper(X object) {
      this.object = object;
    }
  }

  private final String name;
  private FinalWrapper<T> wrapper = null;
  private boolean creating = false;

  @Override
  public T get() {
    FinalWrapper<T> w = wrapper;
    if (w == null) {
      synchronized (this) {
        if (wrapper == null) {
          if (creating) {
            throw new RuntimeException("Circular dependency or failed initialization for " + name);
          }
          creating = true;
          wrapper = new FinalWrapper<>(checkNotNull(doCreate(), "Null %s", name));
        }
        w = wrapper;
      }
    }

    return w.object;
  }

  private T doCreate() {
    try {
      return create();
    } catch (RuntimeException e) {
      // Avoid wrapping RuntimeException in a RuntimeException
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error initializing " + name, e);
    }
  }
}