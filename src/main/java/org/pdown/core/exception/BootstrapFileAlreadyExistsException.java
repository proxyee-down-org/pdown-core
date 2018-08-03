package org.pdown.core.exception;

public class BootstrapFileAlreadyExistsException extends BootstrapException {

  public BootstrapFileAlreadyExistsException() {
    super();
  }

  public BootstrapFileAlreadyExistsException(String message) {
    super(message);
  }

  public BootstrapFileAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public BootstrapFileAlreadyExistsException(Throwable cause) {
    super(cause);
  }
}
