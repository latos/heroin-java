// Copyright 2013 Helix Technologies Australia Pty. Ltd.

package au.com.helixta.inject;

/**
 * Exception thrown when a dependency injection fails
 *
 * @author dan
 */
@SuppressWarnings("serial")
public class InjectionException extends RuntimeException {

  public InjectionException() {
    super();
  }

  public InjectionException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public InjectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InjectionException(String message) {
    super(message);
  }

  public InjectionException(Throwable cause) {
    super(cause);
  }
}
