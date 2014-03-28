// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject;

import au.com.helixta.common.base.Lazies;
import au.com.helixta.common.base.Lazy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 *
 * @author dan
 */
public class Scope implements ScopeCloser {
  private final Object lock = new Object();

  protected volatile boolean closing = false;

  private final List<AutoCloseable> objectsToClose = Lists.newArrayList();

  @Override
  public void close() {
    synchronized (lock) {
      if (closing) {
        return;
      }
      closing = true;

      RuntimeException problem = null;

      for (AutoCloseable object : objectsToClose) {
        if (object == this) {
          continue;
        }
        if (object == null) {
          continue;
        }

        try {
          object.close();
        } catch (Exception e) {
          // Don't throw straight away, finish closing as best we can
          if (problem == null) {
            problem = new RuntimeException("Close problem", e);
          } else {
            problem.addSuppressed(e);
          }
        }
      }

      if (problem != null) {
        throw problem;
      }
    }
  }

  /**
   * ScopeCloser provides scope's own closer.
   */
  public final Lazy<ScopeCloser> scopeCloser = Lazies.<ScopeCloser>eager(this);

  protected void addObjectToClose(AutoCloseable object) {
    objectsToClose.add(object);
  }

  protected void checkOpen() {
    Preconditions.checkState(!closing, "Scope is closed");
  }
}
