// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;
import javax.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the qualifier annotations for a type. Immutable.
 *
 * <p>
 * "Qualifier annotations" are any annotation type that are themselves annotated
 * with {@link Qualifier}.
 *
 * <p>
 * Constructors accept a variety of annotation instances or classes, and
 * conveniently filter out all non-Qualifier annotations.
 *
 * @author dan
 */
final class Qualifiers { // implements Iterable<Class<? extends Annotation>> {
  private final List<Class<? extends Annotation>> classes = new ArrayList<>(); // XXX Make immutable list
  private final List<String> declarations = new ArrayList<>(); // same
  private final List<String> identifiers = new ArrayList<>(); // same

  Qualifiers() {
  }

  Qualifiers(Annotation[] annotations) {
    for (Annotation a : annotations) {
      if (maybeAddNamingQualifier(a)) {
        continue;
      }

      maybeAddParamaterlessQualifier(a.annotationType());
    }
  }

  Qualifiers(Class<? extends Annotation>[] array) {
    this(Arrays.asList(array));
  }

  Qualifiers(List<Class<? extends Annotation>> annotations) {
    for (Class<? extends Annotation> a : annotations) {
      maybeAddParamaterlessQualifier(a);
    }
  }

  public boolean containsType(Class<? extends Annotation> c) {
    return classes.contains(c);
  }

  private void maybeAddParamaterlessQualifier(Class<? extends Annotation> c) {
    if (isQualifier(c)) {
      identifiers.add(c.getSimpleName());
      declarations.add("@" + c.getSimpleName());
      classes.add(c);
    }
  }

  private boolean maybeAddNamingQualifier(Annotation a) {
    String maybeStringValue = getStringValue(a);
    if (maybeStringValue == null) {
      return false;
    }
    String simpleName = a.annotationType().getSimpleName();
    identifiers.add(simpleName + "_" + namedStr(maybeStringValue));
    declarations.add("@" + simpleName + "(\"" + maybeStringValue + "\")");
    classes.add(a.annotationType());
    return true;
  }

  @Nullable
  private static String getStringValue(Annotation a) {
    if (!isQualifier(a.annotationType())) {
      return null;
    }
    try {
      Method valueMethod = a.getClass().getMethod("value");
      if (valueMethod.getReturnType() != String.class) {
        return null;
      }
      return (String) valueMethod.invoke(a);
    } catch (NoSuchMethodException e) {
      return null;
    } catch (IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | SecurityException e) {
      throw new Error(e);
    }
  }

  private static boolean isQualifier(Class<? extends Annotation> c) {
    return c.getAnnotation(Qualifier.class) != null;
  }

  private static String namedStr(String name) {
    // NOTE(dan): This restriction could be relaxed in the future.
    // Restriction is to allow direct mapping to java identifiers without unreadably munging.
    checkArgument(name.matches("^[a-zA-Z0-9-]*$"), "Invalid named string " + name);
    return name.replace('-', '_');
  }

  List<String> getIdentifiers() {
    return new ArrayList<>(identifiers);
  }

  List<String> getDeclarations() {
    return new ArrayList<>(declarations);
  }

  /**
   * Useful for imports, do not rely on for equality or correctness, which
   * depends on specific annotation behaviour.
   */
  List<Class<? extends Annotation>> annotationClasses() {
    return new ArrayList<>(classes);
  }

  public boolean isEmpty() {
    return identifiers.isEmpty();
  }

//  @Override
//  public Iterator<Class<? extends Annotation>> iterator() {
//    return view.iterator();
//  }

  @Override
  public int hashCode() {
    return identifiers.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Qualifiers)) {
      return false;
    }
    Qualifiers other = (Qualifiers) obj;
    return identifiers.equals(other.identifiers);
  }
}