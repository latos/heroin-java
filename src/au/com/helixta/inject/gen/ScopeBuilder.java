// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import au.com.helixta.gen.ClassGenerator;
import au.com.helixta.gen.ClassGenerator.ClassBuilder;
import au.com.helixta.inject.Provides;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scope generation configurator with a fluent-style interface.
 *
 * @author dan
 */
public final class ScopeBuilder {

  private final Package pkg;
  private final String scopeName;
  private final List<Class<?>> providerClasses = new ArrayList<>();
  private final List<QualifiedType> directValues = new ArrayList<>();
  private final List<Type> autoProvided = new ArrayList<>();
  private final List<Satisfier> satisfiers = new ArrayList<>();
  private final List<QualifiedType> factories = new ArrayList<>();
  private final List<QualifiedType> injectedMethodClasses = new ArrayList<>();

  private ScopeBuilder(Package pkg, String scopeName) {
    this.pkg = pkg;
    this.scopeName = scopeName;
  }

  /**
   * Builder starting point.
   *
   * @param pkg package in which scope will be generated
   * @param scopeName class name
   */
  public static ScopeBuilder create(Package pkg, String scopeName) {
    return new ScopeBuilder(pkg, scopeName);
  }

  /**
   * Require that the generated scope provides the given type using a fixed
   * instance that it will receive in its constructor.
   */
  @SafeVarargs
  public final ScopeBuilder instance(Type type, Class<? extends Annotation>... annotations) {
    return instance(new QualifiedType(type, new Qualifiers(annotations)));
  }

  ScopeBuilder instance(QualifiedType type) {
    directValues.add(type);
    return this;
  }

  /**
   * Same as calling {@link #instance(Type, Class...)} for each given argument,
   * with no qualifying annotations.
   *
   * <p>
   * To use qualifying annotations, call {@link #instance(Type, Class...)} for
   * each type.
   */
  public ScopeBuilder instances(Type... types) {
    for (Type t : types) {
      instance(t);
    }
    return this;
  }

  /**
   * Require that the generated scope will automatically provide instances of
   * the given type by lazily constructing purely based on constructor arguments
   * in the obvious way.
   */
  public final ScopeBuilder constructor(Type type) {
    autoProvided.add(type);
    return this;
  }

  /**
   * Same as multiple calls to {@link #constructor(Type)}
   */
  public final ScopeBuilder constructors(Type... types) {
    autoProvided.addAll(Arrays.asList(types));
    return this;
  }

  /**
   * "with" clauses of the satisfy dsl. See
   * {@link ScopeBuilder#satisfy(Type, Class...)}
   */
  public final class SatisfyWith {
    private final QualifiedType dependency;
    private SatisfyWith(QualifiedType dependency) {
      this.dependency = dependency;
    }

    /**
     * @param type
     * @param annotations optional qualifiers
     * @return the scope builder for ongoing building
     */
    @SafeVarargs
    public final ScopeBuilder with(Type type, Class<? extends Annotation>... annotations) {
      satisfiers.add(new Satisfier(dependency, new QualifiedType(type, annotations)));
      return ScopeBuilder.this;
    }

    /**
     * Same as calling both {@link #with(Type, Class...)} and
     * {@link ScopeBuilder#instance(Type, Class...)} with the same args.
     */
    @SafeVarargs
    public final ScopeBuilder withInstance(
        Type type, Class<? extends Annotation>... annotations) {
      QualifiedType qual = new QualifiedType(type, annotations);

      satisfiers.add(new Satisfier(dependency, qual));
      return instance(qual);
    }

    /**
     * Same as calling both {@link #with(Type, Class...)} and
     * {@link ScopeBuilder#constructor(Type)} with the same input type.
     */
    public final ScopeBuilder withConstructor(Type type) {
      satisfiers.add(new Satisfier(dependency, new QualifiedType(type)));
      return constructors(type);
    }
  }

  /**
   * Specifies that a dependency on a given qualified type can be satisfied by
   * an instance of another qualified type.
   *
   * <p>
   * Usually the satisfying type will be a subclass of the dependency (e.g. the
   * "FooInterface" should be satisfied with the "SpecialFooImpl"), and/or they
   * may be annotated differently (e.g. to specify that the "@Foo String" can be
   * satisfied using the "@Bar String").
   *
   * <p>
   * Example usages:
   *
   * <pre>
   * create(pkg, "MyScope")
   *   // Use MyFooImpl for Foo. How to construct MyFooImpl is specified elsewhere.
   *   // Perhaps MyFooImpl has a provider method defining its construction.
   *   .satisfy(Foo.class).with(MyFooImpl.class)
   *
   *   // Use "@ProductionConfig Context" for "Context"
   *   .satisfy(Context.class).with(Context.class, ProductionConfig.class)
   *
   *   // Use "@SettingB String" for "@SettingA String"
   *   .satisfy(String.class, SettingA.class).with(String.class, SettingB.class)
   *
   *   // Use BarImpl for Bar. BarImpl will be constructed automatically by looking
   *   // up the dependencies based on its constructor.
   *   .satisfy(Bar.class).withConstructor(BarImpl.class)
   *
   *   // For Baz, use the instance of BazImpl given to the scope during
   *   // its construction.
   *   .satisfy(Baz.class).withInstance(BazImpl.class)
   * </pre>
   *
   * @param annotations
   *          optional qualifiers
   */
  @SafeVarargs
  public final SatisfyWith satisfy(Type type, Class<? extends Annotation>... annotations) {
    return new SatisfyWith(new QualifiedType(type, annotations));
  }

  /**
   * For each method in each given provider class that is annotated with
   * {@link Provides}, this scope will provide its return value, lazily
   * constructed.
   */
  public ScopeBuilder providers(Class<?>... providers) {
    for (Class<?> c : providers) {
      providerClasses.add(c);
    }
    return this;
  }

  /**
   * Generate an implementation of the given type, which is assumed to be a
   * factory interface.
   *
   * <p>
   * Currently, the interface must have exactly one declared method (TODO:
   * support generating multiple methods).
   *
   * <p>
   * The generated factory will satisfy the dependency on the factory type
   * qualified by any supplied annotations.
   */
  @SafeVarargs
  public final ScopeBuilder factory(Type type, Class<? extends Annotation>... annotations) {
    factories.add(new QualifiedType(type, annotations));
    return this;
  }


  /**
   * Generate method injectors for annotated methods in the given qualified
   * type.
   *
   * <p>
   * Method annotations looked for are either
   * {@link au.com.helixta.inject.InjectMethod} or {@link javax.inject.Inject}.
   * In the latter case, the behavior is the same as {@code InjectMethod} with
   * {@link au.com.helixta.common.base.Returner} as the parameter (i.e.
   * retrieve all arguments from the scope).
   *
   * <p>
   * Two objects will be generated, the injector itself as an instance variable on the
   * scope object, and one static function that takes a scope and returns the injector.
   *
   * <p>
   * See {@link au.com.helixta.inject.InjectMethod} for details
   *
   * @param type
   *          receiver type for which to generate method injectors. The
   *          (possibly qualified) instance on which the methods will be called
   *          needs to be part of the scope (otherwise will be exposed as a
   *          dependency of the scope).
   * @param annotations
   *          optional qualifiers for the receiver type.
   */
  @SafeVarargs
  public final ScopeBuilder injectMethods(Type type, Class<? extends Annotation>... annotations) {
    injectedMethodClasses.add(new QualifiedType(type, annotations));
    return this;
  }

  /**
   * Convenience combination of {@link #constructor(Type)} and
   * {@link #injectMethods(Type, Class...)} on the same type.
   */
  public final ScopeBuilder ctorAndInjectMethods(Type type) {
    constructor(type);
    injectMethods(type);
    return this;
  }

  /**
   * Generates the scope.
   */
  public ClassBuilder generate(ClassGenerator generator) throws IOException {
    return new ScopeGenerator(
      scopeName,
      directValues,
      autoProvided,
      satisfiers,
      providerClasses,
      factories,
      injectedMethodClasses
      ).generate(generator, pkg);
  }
}
