package io.pdown.core.exception;

public class BootstrapBuildException extends RuntimeException {

  public BootstrapBuildException() {
    super();
  }

  public BootstrapBuildException(String message) {
    super(message);
  }

  public BootstrapBuildException(String message, Throwable cause) {
    super(message, cause);
  }

  public BootstrapBuildException(Throwable cause) {
    super(cause);
  }
}
