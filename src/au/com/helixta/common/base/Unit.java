// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.common.base;

/**
 * Similar to {@link java.lang.Void} but useful when a non-null value is
 * desired.
 *
 * @author dan
 */
public final class Unit {

  private Unit() {
  }

  /**
   * The only instance.
   */
  public static final Unit UNIT = new Unit();

  @Override
  public String toString() {
    return "(Unit)";
  }
}
