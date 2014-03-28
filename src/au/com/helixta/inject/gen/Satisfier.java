// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Specifies that a dependency on a given qualified type can be satisfied by an
 * instance of another qualified type.
 *
 * Usually the satisfying type will be a subclass of the dependency (e.g. the
 * "FooInterface" should be satisfied with the "SpecialFooImpl"), and/or they
 * may be annotated differently (e.g. to specify that the "@Foo String" can be
 * satisfied using the "@Bar String").
 *
 * @see ScopeBuilder#satisfy(java.lang.reflect.Type, Class...)
 */
class Satisfier {
  final QualifiedType dependency;
  final QualifiedType satisfiedBy;
  public Satisfier(QualifiedType dependency, QualifiedType satisfiedBy) {
    this.dependency = dependency;
    this.satisfiedBy = satisfiedBy;
    checkArgument(dependency.isAssignableFrom(satisfiedBy),
        dependency + " is not assignable from " + satisfiedBy);
  }
}