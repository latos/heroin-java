// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject.gen;

import static au.com.helixta.gen.GenUtil.asClass;
import static au.com.helixta.gen.GenUtil.getSimpleName;
import static au.com.helixta.gen.GenUtil.sortedMethods;
import static au.com.helixta.inject.gen.Dependency.firstToLower;

import au.com.helixta.common.base.Lazies;
import au.com.helixta.common.base.Lazy;
import au.com.helixta.common.base.Returner;
import au.com.helixta.common.base.StringUtil;
import au.com.helixta.common.base.ThreadsafeLazy;
import au.com.helixta.common.base.Unit;
import au.com.helixta.gen.ClassGenerator;
import au.com.helixta.gen.ClassGenerator.ClassBuilder;
import au.com.helixta.gen.ClassWriter;
import au.com.helixta.inject.InjectMethod;
import au.com.helixta.inject.Provides;
import au.com.helixta.inject.Scope;
import au.com.helixta.inject.gen.Dependency.AutoFactoryDependency;
import au.com.helixta.inject.gen.Dependency.DirectDependency;
import au.com.helixta.inject.gen.Dependency.LazyDependency;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import javax.inject.Inject;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 * @author dan
 */
public class ScopeGenerator {
  static class SatisfactionVar {
    final DirectDependency dependency;
    final DirectDependency satisfiedBy;
    public SatisfactionVar(DirectDependency dependency, DirectDependency satisfiedBy) {
      this.dependency = dependency;
      this.satisfiedBy = satisfiedBy;
    }
  }

  static class ProviderVar {
    final Class<?> klass;
    final String name;
    public ProviderVar(Class<?> klass) {
      this.klass = klass;
      this.name = firstToLower(klass.getSimpleName());
    }

    @Override public String toString() {
      return name;
    }
  }

  static class ProviderCallable {
    final boolean isConstructor;
    final QualifiedType type;
    final String callPart;
    final List<QualifiedType> args;
    /** Null for {@code this} */
    public ProviderCallable(
        boolean isConstructor,
        String callPart,
        QualifiedType type,
        Type[] argTypes,
        Annotation[][] argAnnotations) {
      this.isConstructor = isConstructor;
      this.type = type;
      this.callPart = callPart;

      assert argTypes.length == argAnnotations.length;
      args = Lists.newArrayList();
      for (int i = 0; i < argTypes.length; i++) {
        args.add(new QualifiedType(argTypes[i], new Qualifiers(argAnnotations[i])));
      }
    }

    boolean mightBeNull() {
      return !isConstructor;
    }
  }

  static class Value {
    final DirectDependency providesDep;
    final String variable;
    public Value(DirectDependency providesDep) {
      this.providesDep = providesDep;
      this.variable = providesDep.scopeVar();
    }
  }

  static class ParentScopeVar {
    final ProviderVar parent;
    final Type type;
    final String name;
    public ParentScopeVar(ProviderVar parent, Type type, String name) {
      this.parent = parent;
      this.type = type;
      this.name = name;
    }
  }

  static class ScopeVar {
    final DirectDependency providesDep;
    final ProviderCallable providerMethod;
    final List<Dependency> requiresDeps;
    final String cacheVar;

    public ScopeVar(DirectDependency providesDep, ProviderCallable providerMethod,
        List<Dependency> requiresDeps) {
      this.providesDep = providesDep;
      this.providerMethod = providerMethod;
      this.requiresDeps = requiresDeps;
      this.cacheVar = providesDep.scopeVar();
    }
  }

  private final String scopeName;
  private final List<Class<?>> providerClasses;
  private final List<QualifiedType> directValues;
  private final List<Satisfier> satisfiers;
  private final List<Type> autoProvided;
  private final List<QualifiedType> factories;
  private final List<QualifiedType> injectedMethodClasses;
  private ClassWriter w;

  public ScopeGenerator(
      String scopeName,
      List<QualifiedType> values,
      List<Type> autoProvided,
      List<Satisfier> satisfiers,
      List<Class<?>> providerClasses,
      List<QualifiedType> factories,
      List<QualifiedType> injectedMethodClasses) {
    this.scopeName = Preconditions.checkNotNull(scopeName);
    this.directValues = new ArrayList<>(values);
    this.autoProvided = new ArrayList<>(autoProvided);
    this.satisfiers = new ArrayList<>(satisfiers);
    this.providerClasses = new ArrayList<>(providerClasses);
    this.factories = new ArrayList<>(factories);
    this.injectedMethodClasses = new ArrayList<>(injectedMethodClasses);
  }


  private final List<Value> values = new ArrayList<>();
  private final List<ScopeVar> scopeVars = new ArrayList<>();
  private final List<SatisfactionVar> satisfactionVars = new ArrayList<>();
  private final List<CurriedCtorFactory> factoryVars = new ArrayList<>();
  private final List<MethodInjector> methodInjectors = new ArrayList<>();
  private final Set<DirectDependency> satisfiedDeps = new LinkedHashSet<>();
  private final Set<DirectDependency> requiredDeps = new LinkedHashSet<>();

  /**
   * @return a builder for compiling the java file, for convenience.
   */
  public ClassBuilder generate(ClassGenerator generator, Package pkg) throws IOException {
    Preconditions.checkState(w == null);
    w = generator.writerFor(pkg, scopeName);

    think();
    emit();

    return w.closeWithBuilder();
  }

  private void think() {
    w.addType(Preconditions.class);
    w.addType(Scope.class);
    w.addType(Lazy.class);
    w.addType(Lazies.class);
    w.addType(ThreadsafeLazy.class);
    // TODO(dan): Bring this back, configure the lazies to throw these
    // w.addType(InjectionException.class);

    for (QualifiedType t : directValues) {
      values.add(new Value(satisfied(importQualifiers(directDepFromType(t)))));
    }

    for (Type t : autoProvided) {
      w.addType(t);
      Class<?> c = asClass(t);
      Constructor<?> toInject = selectConstructor(c);
      System.out.println("Auto provider " + c.getName());

      addLazyVar(new ProviderCallable(
          true, // is constructor
          "new " + c.getSimpleName(),
          new QualifiedType(
              t, new Qualifiers() /* no annotations */),
          toInject.getGenericParameterTypes(),
          toInject.getParameterAnnotations()));
    }

    for (Class<?> providerClass : providerClasses) {
      w.addType(providerClass);
      ProviderVar providerVar = new ProviderVar(providerClass);
      addDependency(new QualifiedType(providerClass));

      for (Method method : sortedMethods(providerClass.getDeclaredMethods())) {
        Provides provides = method.getAnnotation(Provides.class);
        if (provides == null) {
          continue;
        }

        if (!Modifier.isPublic(method.getModifiers())) {
          throw new RuntimeException("Provider method " + method.getName() + " must be public");
        }

        System.out.println("Provider " + method.getName());

        Qualifiers annotations = new Qualifiers(method.getAnnotations());

        addLazyVar(new ProviderCallable(
            false, // is not constructor
            providerVar.name + ".get()." + method.getName(),
            new QualifiedType(
                method.getGenericReturnType(),
                annotations),
            method.getGenericParameterTypes(),
            method.getParameterAnnotations()));
      }
    }

    for (QualifiedType typeWithMethods : injectedMethodClasses) {
      for (Method method : sortedMethods(asClass(typeWithMethods.type).getMethods())) {
        InjectMethod inject = method.getAnnotation(InjectMethod.class);
        Type iface;
        if (inject == null) {
          if (method.getAnnotation(Inject.class) != null) {
            Type retType = method.getGenericReturnType();
            if (retType == void.class) {
              retType = Unit.class;
            }
            iface = new ParameterizedTypeImpl(Returner.class, retType);
          } else {
            continue;
          }
        } else {
          iface = inject.value();
        }

        System.out.println("Method Injector for " + method.getName());

        methodInjectors.add(new MethodInjector(typeWithMethods, method, iface));
      }
    }

    for (Satisfier s : satisfiers) {
      satisfactionVars.add(new SatisfactionVar(
          satisfied(directDepFromType(s.dependency)),
          directDepFromType(s.satisfiedBy)));
    }

    // Use a while-loop because auto-factories may be added as dependencies
    // while generating other factories. Since auto-factories are (currently)
    // the only implicitly generated dependency, it is sufficient to place
    // factory generation last and use this approach. If it becomes possible
    // for anything else to be explicitly generated, it may become necessary
    // to do a non-trivial refactor of this class.
    while (!factories.isEmpty()) {
      QualifiedType f = factories.remove(0);
      Dependency dep = depFromType(f);
      if (satisfiedDeps.contains(dep)) {
        continue;
      }
      satisfied(dep);
      CurriedCtorFactory fg = new CurriedCtorFactory(f);
      assert fg.dependency.equals(dep);
      factoryVars.add(fg);
    }

    // Finally, expose all unsatisfied dependencies as constructor arguments.
    // Note, these are currently forced eagerly evaluated.
    // TODO(dan): Pass them through as Lazy values instead.
    for (DirectDependency dep : requiredDeps) {
      if (!satisfiedDeps.contains(dep)) {
        values.add(new Value(importQualifiers(dep)));
      }
    }
  }

  private <T extends Dependency> T satisfied(T dep) {
    satisfiedDeps.add(dep.getDirectDep());
    return dep;
  }

  class CurriedCtorFactory {
    final DirectDependency dependency;
    // TODO(dan): This guava typetoken stuff is pretty nice. Consider
    // changing all generator code to use TypeToken, Invokable, etc
    // instead of Type, Method, etc.
    final TypeToken<?> returnType;
    final Invokable<?, ?> implementedMethod;
    final CurriedArgs args;

    CurriedCtorFactory(QualifiedType factoryType) {
      this.dependency = directDepFromType(factoryType);
      implementedMethod = getSingleMethod(factoryType.type);
      returnType = implementedMethod.getReturnType();
      w.addType(returnType.getType());
      Invokable<?, ?> ctor = returnType.constructor(selectConstructor(returnType.getRawType()));
      args = new CurriedArgs(implementedMethod, ctor);
    }
  }

  class MethodInjector {
    final DirectDependency receiver;
    final Invokable<?, ?> innerMethod;
    final Type iface;
    final TypeToken<?> returnType;
    final Invokable<?, ?> implementedMethod;
    final CurriedArgs args;
    final String varName;
    public MethodInjector(QualifiedType receiverType, Method method, Type interfaceType) {
      w.addType(Function.class);
      this.receiver = directDepFromType(receiverType);
      this.innerMethod = TypeToken.of(receiverType.type).method(method);
      this.iface = w.addType(interfaceType);
      this.implementedMethod = getSingleMethod(iface);
      TypeToken<?> retType = implementedMethod.getReturnType();
      if (retType.getType() instanceof TypeVariable) {
        retType = innerMethod.getReturnType();
      }
      this.returnType = retType;
      this.args = new CurriedArgs(implementedMethod, innerMethod);

      // TODO: handle overloaded methods without making the variable name too grotesquely verbose.
      //       and disambiguate qualified receiver types.
      this.varName = asClass(receiverType.type).getSimpleName() + "_" + method.getName();

      w.addType(returnType.getType());
    }
  }

  static Invokable<?, ?> getSingleMethod(Type type) {
    Method[] methods = asClass(type).getDeclaredMethods();
    if (methods.length == 0) {
      throw new UnsupportedOperationException("must define method");
    }

    if (methods.length > 1) {
      // TODO: ways to detect the actual factory method among others, and/or
      // support multiple methods.
      throw new UnsupportedOperationException("only one method currently supported");
    }

    return TypeToken.of(type).method(methods[0]);
  }


  class CurriedArgs {
    final List<QualifiedType> implementedArgs = new ArrayList<>();
    final List<Dependency> curriedDeps = new ArrayList<>();

    CurriedArgs(Invokable<?, ?> implementedMethod, Invokable<?, ?> innerInvokable) {
      for (Parameter p : implementedMethod.getParameters()) {
        implementedArgs.add(qualtypeFrom(p));
      }

      for (Parameter p : innerInvokable.getParameters()) {
        QualifiedType paramType = qualtypeFrom(p);
        w.addType(paramType.type);

        // If this is a direct, uncurried arg
        if (implementedArgs.contains(paramType)) {
          // Import the annotations, as we'll be declaring them
          // in the factory implementation for completeness.
          for (Annotation a : p.getAnnotations()) {
            w.addType(a.annotationType());
          }
          curriedDeps.add(null); // semi-hack: placeholder for arg
          continue;
        }

        curriedDeps.add(depFromType(paramType));
      }
    }
  }

  private static QualifiedType qualtypeFrom(Parameter p) {
    return new QualifiedType(p.getType().getType(), p.getAnnotations());
  }

  private static Constructor<?> selectConstructor(Class<?> c) {
    Constructor<?>[] ctors = c.getConstructors();
    if (ctors.length == 1) {
      return ctors[0];
    } else {
      Constructor<?> found = null;
      for (Constructor<?> ctor : ctors) {
        if (ctor.getAnnotation(Inject.class) != null) {
          if (found != null) {
            throw new RuntimeException("Cannot have more than one @Inject for " + c.getName());
          }
          found = ctor;
        }
      }
      if (found != null) {
        return found;
      }
      throw new RuntimeException("Ambiguous constructor selection, resolve with @Inject "
          + " or declare only one public constructor for " + c.getName());
    }
  }

  private void addLazyVar(ProviderCallable providerMethod) {

    // Providers may only provide direct dependencies
    DirectDependency providedDep = satisfied(directDepFromType(providerMethod.type));
    scopeVars.add(new ScopeVar(providedDep, providerMethod,
        getDepsFromAnnotatedTypes(providerMethod.args)));
  }

  List<Dependency> getDepsFromAnnotatedTypes(List<QualifiedType> types) {
    List<Dependency> deps = Lists.newArrayList();
    for (QualifiedType t : types) {
      deps.add(depFromType(t));
    }
    return deps;
  }

  <T extends Dependency> T importQualifiers(T dep) {
    for (Class<? extends Annotation> q : dep.outerType().qualifiers.annotationClasses()) {
      w.addType(q);
    }
    return dep;
  }

  DirectDependency directDepFromType(QualifiedType t) {
    Dependency dep = depFromType(t);
    if (dep instanceof LazyDependency) {
      throw new UnsupportedOperationException("Type " + t + " resulted in a Lazy dep " +
          "when this context only permits a direct dep");
    }
    return (DirectDependency) dep;
  }

  Dependency depFromType(QualifiedType t) {
    w.addType(t.type);
    Dependency d = Dependency.fromType(t);
    requiredDeps.add(d.getDirectDep());

    if (d instanceof AutoFactoryDependency) {
      if (!satisfiedDeps.contains(d)) {
        factories.add(t);
      }
    }
    return d;
  }

  void addDependency(QualifiedType t) {
    depFromType(t);
  }

  private void emit() {
    w.writeHeader();

    // Consider using some simple template lib, like
    // https://code.google.com/p/jmte/

    w("/** Generated scope. */");
    w("public class " + scopeName + " extends Scope {");
    w("  private final " + scopeName + " self = this;");
    if (!values.isEmpty()) {
      w("");
      w("  // Direct values");
    }
    for (Value v : values) {
      w("  /** " + v.providesDep + " */");
      w("  public final Lazy<" + v.providesDep.simpleTypeName() + "> " + v.variable + ";");
    }
    if (!satisfactionVars.isEmpty()) {
      w("");
      w("  // Satisfiers");
    }
    for (SatisfactionVar s : satisfactionVars) {
      w("  Lazy<" + s.dependency.simpleTypeName() + "> " + s.dependency.scopeVar() + ";");
    }


    // CONSTRUCTOR

    w("");
    w("  /** Declares seed dependencies of this scope. */");
    w("  public " + scopeName + "(");
    {
    List<String> args = Lists.newArrayList();
      for (Value v : values) {
        args.add("      " + renderQualifiedType(v.providesDep.outerType()) + " " + v.variable);
      }
      w(StringUtil.joinIterable(",\n", args) + ") {");
    }
    for (Value var : values) {
      w("    this." + var.variable + " = Lazies.eager("
          + var.variable + ", \"" + var.variable + "\");");
    }
    for (SatisfactionVar s : satisfactionVars) {
      w("    " + s.dependency.scopeVar()
          + " = Lazies.<" + s.dependency.simpleTypeName()
              + ">upcast(this." + s.satisfiedBy.scopeVar() + ");");
    }
    w("  }");
    w("");

    // END CONSTRUCTOR

    for (ScopeVar g : scopeVars) {
      boolean isAutoCloseable = AutoCloseable.class.isAssignableFrom(
          asClass(g.providesDep.outerType().type));

      String type = g.providesDep.simpleTypeName();
      w("  /** " + g.providesDep + " */");
      w("  public final Lazy<" + type + "> " + g.cacheVar + " =");
      String escapedDescriptor = g.providesDep.toString().replace("\"", "\\\"");
      w("    new ThreadsafeLazy<" + type +">(\"" + escapedDescriptor + "\") { ");
      w("    @Override protected " + type + " create() throws Exception {");
      w("      checkOpen();");
      w("      " + type + " object = " + g.providerMethod.callPart + "("
          + (g.requiresDeps.isEmpty() ? ");" : ""));
      for (int i = 0; i < g.requiresDeps.size(); i++) {
        // TODO(dan): There is the very remote possibility of a deadlock
        // here. It would be more correct if the args were initialised
        // into separate variables, in member variable order (not arg order).
        Dependency dep = g.requiresDeps.get(i);
        w("        " + dep.instantiationExpr("self")
            + (i == g.requiresDeps.size() - 1 ? ");" : ","));
      }
      if (isAutoCloseable) {
        w("      addObjectToClose(object);");
      }
      w("      return object;");
      w("    }");
      w("  };");
      w("");
    }

    for (CurriedCtorFactory f : factoryVars) {
      String type = f.dependency.simpleTypeName();
      w("  /** " + f.dependency + " */");
      w("  public final Lazy<" + type + "> " + f.dependency.scopeVar()
          + " = Lazies.<" + type + ">eager(new " + type + "() {");
      w("    @Override public " + getSimpleName(f.returnType) + " "
          + f.implementedMethod.getName() + "(");
      int arg = 0;
      List<String> args = new ArrayList<>();
      for (QualifiedType p : f.args.implementedArgs) {
        args.add(renderQualifiedType(p) + " arg" + (arg++));
      }
      w("        " + StringUtil.joinIterable(", ", args) + ")/*XXX*/ {");
      w("      return new " + getSimpleName(f.returnType) + "(");
      arg = 0;
      args = new ArrayList<>();
      for (Dependency d : f.args.curriedDeps) {
        if (d == null) {
          args.add("arg" + (arg++));
        } else {
          args.add(d.instantiationExpr("self"));
        }
      }
      w("        " + StringUtil.joinIterable(",  ", args) + ");");
      w("    }");
      w("  });");
      w("");
    }

    for (MethodInjector mi : methodInjectors) {
      w("  /** " + mi.receiver.simpleTypeName() + "#" + mi.innerMethod.getName() + "() */");
      String iface = getSimpleName(mi.iface);
      w("  public final " + iface + " " + mi.varName
          + " = new " + iface + "() {");
      wn("    @Override public " + getSimpleName(mi.returnType) + " "
          + mi.implementedMethod.getName() + "(");
      int arg = 0;
      List<String> args = new ArrayList<>();
      for (QualifiedType p : mi.args.implementedArgs) {
        args.add(renderQualifiedType(p) + " arg" + (arg++));
      }
      w(StringUtil.joinIterable(", ", args) + ") {");

      boolean returningUnit = (mi.returnType.getType() == Unit.class);
      w("      " + (returningUnit ? "" : "return ") + mi.receiver.instantiationExpr("self")
          + "." + mi.innerMethod.getName() + "(");
      arg = 0;
      args = new ArrayList<>();
      for (Dependency d : mi.args.curriedDeps) {
        if (d == null) {
          args.add("arg" + (arg++));
        } else {
          args.add(d.instantiationExpr("self"));
        }
      }
      w("        " + StringUtil.joinIterable(",  ", args) + ");");
      if (returningUnit) {
        w("      return Unit.UNIT;");
      }
      w("    }");
      w("  };");
      w("  /** static " + mi.receiver.simpleTypeName() + "#" + mi.innerMethod.getName() + "() */");
      String staticType = "Function<" + scopeName + ", " + iface + ">";
      w("  public static final " + staticType + " " + mi.varName.toUpperCase()
          + " = new " + staticType + "() {");
      w("    @Override public " + iface + " apply(" + scopeName + " scope) {");
      w("      return scope." + mi.varName + ";");
      w("    }");
      w("  };");
    }

    w("  // Avoid unused import warning");
    w("  static { Preconditions.checkNotNull(new Object()); }");
    w("}");
  }

  private static String renderQualifiedType(QualifiedType q) {
    List<String> paramAnnotations = new ArrayList<>();
    for (String decl : q.qualifiers.getDeclarations()) {
      paramAnnotations.add(decl + " ");
    }
    return StringUtil.joinIterable(" ", paramAnnotations) + getSimpleName(q.type);
  }

  private void w(String str) {
    w.println(str);
  }

  private void wn(String str) {
    w.print(str);
  }

}

