// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import au.com.helixta.common.base.Factories.Factory0;

/**
 * Collection of Lazy implementations and helpers.
 *
 * @author dan
 */
public class Lazies {
  private Lazies() {
  }

  public static <T> Lazy<T> threadsafe(final Factory0<T> f) {
    return new ThreadsafeLazy<T>() {
      @Override
      protected T create() {
        return f.create();
      }
    };
  }

  public static <T> Lazy<T> simple(final Factory0<T> f) {
    return new SimpleLazy<T>() {
      @Override
      protected T create() {
        return f.create();
      }
    };
  }


  public static <T> Lazy<T> eager(T obj) {
    return new Eager<>(obj);
  }

  public static <T> Lazy<T> eager(T obj, String name) {
    return new Eager<>(obj, name);
  }

  /**
   * Upcast helper. Because Lazies are readonly, this is safe to do.
   */
  @SuppressWarnings("unchecked")
  public static <T> Lazy<T> upcast(Lazy<? extends T> lazy) {
    return (Lazy<T>) lazy;
  }

  public static abstract class SimpleLazy<T> implements Lazy<T> {
    private T value = null;

    protected abstract T create() throws Exception;

    @Override
    public T get() {
      if (value == null) {
        try {
          value = create();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      return value;
    }
  }

  /**
   * Not actually lazy!
   */
  public static class Eager<T> implements Lazy<T> {
    private final T object;

    public Eager(T object) {
      this.object = checkNotNull(object);
    }

    public Eager(T object, String name) {
      this.object = checkNotNull(object, "Null %s", name);
    }


    @Override
    public T get() {
      return object;
    }
  }
}
