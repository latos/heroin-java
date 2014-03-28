// Copyright 2011 Helix Technologies Australia.

package au.com.helixta.common.base;

import javax.annotation.Nullable;

/**
 *
 * @author dan@helixta.com.au
 */
public final class StringUtil {
  private StringUtil() {}

  public static String chomp(String input) {
    int last = input.length() - 1;
    if (input.charAt(last) == '\n') {
      return input.substring(0, last);
    } else {
      return input;
    }
  }

  public static String join(String sep, Object... strings) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (Object s : strings) {
      if (!first) {
        b.append(sep);
      } else {
        first = false;
      }

      b.append(s);
    }
    return b.toString();
  }

  public static String joinIterable(String sep, Iterable<?> strings) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (Object o : strings) {
      if (!first) {
        b.append(sep);
      } else {
        first = false;
      }

      b.append(o);
    }
    return b.toString();
  }

  public static String repeat(int times, String sep, String str) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (int i = 0; i < times; i++) {
      if (!first) {
        b.append(sep);
      } else {
        first = false;
      }

      b.append(str);
    }
    return b.toString();
  }

  public static boolean isBlank(@Nullable String s) {
    return s == null || s.isEmpty();
  }
}
