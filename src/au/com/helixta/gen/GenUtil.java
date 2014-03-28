// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.gen;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 *
 * @author dan
 */
public class GenUtil {
  private GenUtil() {}

  @SuppressWarnings("rawtypes")
  public static Class<?> asClass(Type t) {
    if (t instanceof Class) {
      return (Class)t;
    } else if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      return (Class) pt.getRawType();
    } else {
      throw new AssertionError("Unimplemented");
    }
  }

  @SuppressWarnings("rawtypes")
  public static List<String> getAllTypeNames(Type t) {
    if (t instanceof Class) {
      Class<?> c = (Class)t;
      return Lists.newArrayList(c.getSimpleName());
    } else if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      Class<?> c = (Class) pt.getRawType();
      List<String> names = Lists.newArrayList(c.getSimpleName());
      for (Type arg : pt.getActualTypeArguments()) {
        names.addAll(getAllTypeNames(arg));
      }
      return names;
    } else {
      throw new AssertionError("Unimplemented");
    }
  }

  public static String getSimpleName(TypeToken<?> t) {
    return getSimpleName(t.getType());
  }

  @SuppressWarnings("rawtypes")
  public static String getSimpleName(Type t) {
    if (t instanceof Class) {
      Class<?> c = (Class)t;
      return c.getSimpleName();
    } else if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      Class<?> c = (Class) pt.getRawType();
      String name = c.getSimpleName() + "<";
      boolean first = true;
      for (Type arg : pt.getActualTypeArguments()) {
        if (first) {
          first = false;
        } else {
          name += ", ";
        }
        name += getSimpleName(arg);
      }
      return name + ">";
    } else if (t instanceof WildcardType) {
      WildcardType wt = (WildcardType) t;
      if (wt.getLowerBounds().length > 0) {
        throw new AssertionError("not implemented: wildcard lower bounds handling");
      }
      if (wt.getUpperBounds().length > 1 || wt.getUpperBounds()[0] != Object.class) {
        throw new AssertionError("not implemented: wildcard upper bounds handling");
      }
      return "?";
    } else {
      throw new AssertionError("Unimplemented " + t);
    }
  }



  /**
   * Returns a sorted copy of the given methods array.
   *
   * <p>
   * When using reflection, class methods are not returned in any deterministic
   * order! This causes the generated file to be different each time, unless we
   * sort.
   *
   * @see <a href="http://bugs.sun.com/view_bug.do?bug_id=7023180">this bug
   *      report</a>
   */
  public static Method[] sortedMethods(Method[] methods) {
    methods = Arrays.copyOf(methods, methods.length);
    Arrays.sort(methods, METHOD_CMP);
    return methods;
  }
  public static final Comparator<Method> METHOD_CMP = new Comparator<Method>() {
    @Override public int compare(Method m1, Method m2) {
      // Method's toString is well defined and includes modifiers and arg types.
      return m1.toString().compareTo(m2.toString());
    }
  };
}
