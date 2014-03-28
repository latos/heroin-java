// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import au.com.helixta.common.base.ObjUtil;
import au.com.helixta.gen.GenUtil;

import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;


/**
 * Represents a type with a list of simple annotations
 *
 * @author dan
 */
class QualifiedType {
  static final Annotation[] token = new Annotation[0];

  final Type type;
  final Qualifiers qualifiers;

  public QualifiedType(Type type) {
    this(type, new Qualifiers());
  }

  public QualifiedType(Type type, Annotation... annotations) {
    this(type, new Qualifiers(annotations));
  }

  public QualifiedType(Type type, List<Annotation> annotations) {
    this(type, new Qualifiers(annotations.toArray(token)));
  }

  @SafeVarargs
  public QualifiedType(Type type, Class<? extends Annotation>... annotations) {
    this(type, new Qualifiers(annotations));
  }

  public QualifiedType(Type type, Qualifiers qualifiers) {
    this.type = type;
    this.qualifiers = qualifiers;
  }

  public Qualifiers qualifiers() {
    return qualifiers;
  }

  @Override public String toString() {
    StringBuilder b = new StringBuilder("[");
    for (String q : qualifiers.getDeclarations()) {
      b.append(q + " ");
    }
    b.append(type.toString());
    b.append("]");
    return b.toString();
  }

  public boolean isAssignableFrom(QualifiedType other) {
    return GenUtil.asClass(type).isAssignableFrom(GenUtil.asClass(other.type));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    QualifiedType other = ObjUtil.compatible(this, obj, true);
    if (other == null) {
      return false;
    }

    return other.type.equals(type) && other.qualifiers.equals(qualifiers);
  }

  @Override
  public int hashCode() {
    throw new AssertionError("Unimplemented QualifiedType.hashCode");
  }
}