package org.pdown.core.exception;

public class BootstrapPathEmptyException extends BootstrapException {

  public BootstrapPathEmptyException() {
    super();
  }

  public BootstrapPathEmptyException(String message) {
    super(message);
  }

  public BootstrapPathEmptyException(String message, Throwable cause) {
    super(message, cause);
  }

  public BootstrapPathEmptyException(Throwable cause) {
    super(cause);
  }
}
