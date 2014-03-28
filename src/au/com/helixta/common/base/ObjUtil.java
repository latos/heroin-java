// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

import javax.annotation.Nullable;

/**
 *
 *
 * @author dan
 */
public class ObjUtil {
  private ObjUtil() {}

  public static <T> T nonnull(@Nullable T obj) {
    if (obj == null) {
      throw new NullPointerException("nonnull fail");
    }
    return obj;
  }

  @Nullable
  public static <T> T compatible(T self, @Nullable Object other, boolean exactType) {
    if (other == null) {
      return null;
    }

    if (exactType) {
      if (self.getClass() != other.getClass()) {
        return null;
      }
    } else {
      if (!self.getClass().isInstance(other)) {
        return null;
      }
    }

    @SuppressWarnings("unchecked")
    T ret = (T) other;
    return ret;
  }
}
