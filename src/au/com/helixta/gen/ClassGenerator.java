// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.gen;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;


/**
 *
 *
 * @author dan
 */
public class ClassGenerator {
  private final String genDir;
  private final String buildDir;


  private final JavaCompiler compiler;
  private final StandardJavaFileManager fileManager;


  public ClassGenerator(String genDir, String buildDir) {
    this.genDir = genDir;
    this.buildDir = buildDir;


    compiler = ToolProvider.getSystemJavaCompiler();
    fileManager = compiler.getStandardFileManager(null, null, null);

  }

  public ClassWriter writerFor(Class<?> klass, String suffix) throws IOException {
    return writerFor(klass.getPackage().getName(),
        klass.getSimpleName() + suffix);
  }

  public ClassWriter writerFor(Package pkg, String name) throws IOException {
    return writerFor(pkg.getName(), name);
  }

  public ClassWriter writerFor(String pkg, String name) throws IOException {
    String components = "/" + pkg.replace('.', '/') + "/" + name;
    File javaFile = new File(genDir + components + ".java");
    return new ClassWriter(this, pkg, name, javaFile);
  }

  // TODO: this is a bit messy, passing classFile around, and this
  // class being an inner class while classwriter is not, we exit
  // and loop back in... mmm...
  public ClassBuilder createBuilder(File javaFile, String className) {
    return new ClassBuilder(javaFile, className);
  }

  // TODO: Rename!
  public class ClassBuilder {
    private final File javaFile;
    private final String className;

    @Nullable private Class<?> built;

    public ClassBuilder(File javaFile, String className) {
      this.javaFile = javaFile;
      this.className = className;
    }

    public Class<?> builtClass() {
      if (built != null) {
        return built;
      }

      Iterable<String> options = Lists.newArrayList("-d", buildDir);

      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjectsFromFiles(Lists.newArrayList(javaFile));
      compiler.getTask(null, fileManager, null, options, null,
          compilationUnits).call();

      try {
        return built = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
