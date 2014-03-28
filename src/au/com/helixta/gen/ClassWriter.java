// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.gen;

import au.com.helixta.gen.ClassGenerator.ClassBuilder;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassWriter extends PrintWriter {
  static final int year = Calendar.getInstance().get(Calendar.YEAR);

  static final AtomicInteger nextId = new AtomicInteger();
  static String nextTmpFile(String prefix) {
    return prefix + ".tmp-" + System.currentTimeMillis()
        + "-" + nextId.incrementAndGet();
  }

  public final String pkg;
  public final String name;

  private final ClassGenerator generator;
  private final File javaFile;
  private final File tmpFile;

  public final Set<String> imports = Sets.newTreeSet();

  private static File ensureDirs(File f) {
    f.getParentFile().mkdirs();
    return f;
  }

  public ClassWriter(ClassGenerator generator, String pkg, String name,
      File javaFile) throws IOException {
    this(generator, pkg, name, new File(nextTmpFile(javaFile.getCanonicalPath())), javaFile);
  }


  private ClassWriter(ClassGenerator generator, String pkg, String name,
      File tmpFile, File javaFile) throws FileNotFoundException {
    super(ensureDirs(tmpFile));
    this.generator = generator;
    this.pkg = pkg;
    this.name = name;

    this.tmpFile = tmpFile;
    this.javaFile = javaFile;
  }

  public void writeHeader() {
    w("// Copyright " + year + " Helix Technologies Australia Pty. Ltd.");
    w("");
    w("package " + pkg + ";");
    w("");
    for (String s : imports) {
      w("import " + s + ";");
    }
    w("");
    w("// GENERATED CLASS - DO NOT EDIT");
  }


  private void w(String s) {
    println(s);
  }

  public void addImport(Class<?> klass) {
    addImport(klass.getCanonicalName());
  }

  public void addImport(String klass) {
    imports.add(klass);
  }

  /**
   * Records the given type and all its type parameters, so they will
   * automatically be imported where necessary.
   *
   * @param t
   * @return t for convenience
   */
  @SuppressWarnings("rawtypes")
  public Type addType(Type t) {
    if (t instanceof Class) {
      Class<?> c = (Class)t;
      if (c.isArray()) {
        c = c.getComponentType();
      }
      if (!c.isPrimitive()) {
        imports.add(c.getCanonicalName());
      }

    } else if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      Class<?> c = (Class) pt.getRawType();
      imports.add(c.getCanonicalName());
      for (Type arg : pt.getActualTypeArguments()) {
        addType(arg);
      }
    } else if (t instanceof WildcardType) {
      WildcardType wt = (WildcardType) t;
      for (Type bound : wt.getLowerBounds()) {
        addType(bound);
      }
      for (Type bound : wt.getUpperBounds()) {
        addType(bound);
      }
    } else {
      throw new AssertionError("Unimplemented " + t.getClass());
    }
    return t;
  }

  @Override
  public void close() {
    // Make the modification atomic
    super.close();
    ensureDirs(javaFile);
    boolean succeeded = tmpFile.renameTo(javaFile);
    if (!succeeded) {
      throw new RuntimeException("Rename failed " + tmpFile + " -> " + javaFile);
    }
  }

  public ClassBuilder closeWithBuilder() {
    close();
    return generator.createBuilder(javaFile, pkg + "." + name);
  }
}