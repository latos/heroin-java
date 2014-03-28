// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import static au.com.helixta.gen.GenUtil.asClass;
import static au.com.helixta.gen.GenUtil.getSimpleName;

import au.com.helixta.common.base.Lazy;
import au.com.helixta.common.base.ObjUtil;
import au.com.helixta.common.base.StringUtil;
import au.com.helixta.gen.GenUtil;
import au.com.helixta.inject.AutoFactory;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;

public abstract class Dependency implements Comparator<Dependency> {

  public static Dependency fromType(QualifiedType type) {
    Type t = type.type;

    boolean isAutoFactory = type.qualifiers().containsType(AutoFactory.class);
    Class<?> c = asClass(t);

    if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      Type[] typeArgs = pt.getActualTypeArguments();

      if (isAutoFactory && Lazy.class.isAssignableFrom(c)) {
        throw new UnsupportedOperationException("Lazy types are not factories");
      }
      if (c == Lazy.class) {
        assert typeArgs.length == 1;

        return new LazyDependency(new DirectDependency(typeArgs[0], type.qualifiers));
      }
    }

    if (isAutoFactory) {
      return new AutoFactoryDependency(t, type.qualifiers);
    }

    return new DirectDependency(t, type.qualifiers);
  }

  public static class DirectDependency extends Dependency {
    public static String descriptor(Type type, Qualifiers annotations) {
      String desc = "";
      if (!annotations.isEmpty()) {
        desc += StringUtil.joinIterable(" ", annotations.getDeclarations()) + " ";
      }
      desc += GenUtil.getSimpleName(type);
      return desc;
    }
    public static String identifier(Type type, Qualifiers annotations) {
      String id = firstToLower(StringUtil.joinIterable("", GenUtil.getAllTypeNames(type)));
      if (!annotations.isEmpty()) {
        // 0 kinda looks like @
        id += "_0" + StringUtil.joinIterable("_0", annotations.getIdentifiers());
      }
      return id;
    }

    private final String identifier;

    public DirectDependency(Type type, Qualifiers annotations) {
      this(new QualifiedType(type, annotations));
    }

    public DirectDependency(QualifiedType type) {
      super(type, descriptor(type.type, type.qualifiers));
      this.identifier = identifier(type.type, type.qualifiers);
    }

    @Override public String instantiationExpr(String scopeVar) {
      return scopeVar + "." + scopeVar() + ".get()";
    }

    @Override
    public DirectDependency getDirectDep() {
      return this;
    }

    public String scopeVar() {
      return identifier;
    }
  }

  /**
   * Subclass simply to mark that this dependency is an auto-factory dep that
   * may be generated without explicit configuration.
   */
  public static class AutoFactoryDependency extends DirectDependency {
    public AutoFactoryDependency(Type type, Qualifiers annotations) {
      super(type, annotations);
    }
  }

  public static String firstToLower(String s) {
    return s.toLowerCase().charAt(0) + s.substring(1);
  }

  public static class LazyDependency extends Dependency {
    public static Type innerType(Type lazyType) {
      ParameterizedType t = (ParameterizedType) lazyType;
      Preconditions.checkArgument(asClass(lazyType) == Lazy.class,
          "Lazy dependency must be of type Lazy<T>");
      return t.getActualTypeArguments()[0];
    }

    public static String descriptor(DirectDependency provided) {
      return "Lazy<" + provided.descriptor() + ">";
    }

    public final DirectDependency provided;
    public LazyDependency(DirectDependency providedDep) {
      super(new QualifiedType(
          new ParameterizedTypeImpl(Lazy.class, providedDep.outerType().type),
              providedDep.outerType().qualifiers),
          descriptor(providedDep));
      provided = providedDep;
    }

    @Override public String instantiationExpr(String scopeVar) {
      return scopeVar + "." + provided.scopeVar();
    }

    @Override
    public DirectDependency getDirectDep() {
      return provided;
    }
  }

  @Nonnull private final QualifiedType outerType;
  private final String descriptor;

  protected Dependency(QualifiedType type, String descriptor) {
    this.outerType = type;
    this.descriptor = descriptor;
  }

  /**
   * @return the associated direct dep (which could be self, or, in the case of
   *         a lazy dep, the boxed dep).
   */
  public abstract DirectDependency getDirectDep();

  public abstract String instantiationExpr(String scopeVariable);

  @Override public String toString() {
    return "Dependency(" + descriptor + ")";
  }

  /**
   * Non-canoncial name of the outer type.
   */
  public String simpleTypeName() {
    return getSimpleName(outerType.type);
  }
  /**
   * @return unambiguous string representation of this dependency
   */
  public String descriptor() {
    return descriptor;
  }

  /**
   * @return the entire declared type of the dependency.
   */
  public QualifiedType outerType() {
    return outerType;
  }

  @Override public final int compare(Dependency o1, Dependency o2) {
    return o1.descriptor.compareTo(o2.descriptor);
  }

  @Override public final boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    Dependency other = ObjUtil.compatible(this, obj, true);
    if (other == null) {
      return false;
    }

    if (descriptor.equals(other.descriptor)) {
      assert outerType.equals(other.outerType);
      return true;
    }

    return false;
  }

  @Override public final int hashCode() {
    return descriptor.hashCode();
  }

}