// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import com.google.common.collect.Lists;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author dan
 */
public class ParameterizedTypeImpl implements ParameterizedType {
  private final Type[] token = new Type[0];

  private final Class<?> rawType;
  private final List<Type> args;

  public ParameterizedTypeImpl(Class<?> rawType, Type... args) {
    this.rawType = rawType;
    this.args = Lists.newArrayList(args);
  }

  @Override
  public Type[] getActualTypeArguments() {
    return args.toArray(token);
  }

  @Override
  public Type getRawType() {
    return rawType;
  }

  @Override
  public Type getOwnerType() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ParameterizedType)) {
      return false;
    }

    if (obj instanceof ParameterizedTypeImpl) {
      ParameterizedTypeImpl other = (ParameterizedTypeImpl) obj;
      return rawType.equals(other.rawType) && args.equals(other.args);
    }

    // TODO: implement comparison to any ParameterizedType... needs to
    // carefully obey the right contract.

    throw new AssertionError("Not Implemented");
  }

  @Override
  public int hashCode() {
    throw new AssertionError("Not Implemented");
  }
}
