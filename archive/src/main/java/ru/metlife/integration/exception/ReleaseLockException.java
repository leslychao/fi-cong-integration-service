package ru.metlife.integration.exception;

public class ReleaseLockException extends RuntimeException {

  public ReleaseLockException() {
  }

  public ReleaseLockException(String message) {
    super(message);
  }

  public ReleaseLockException(Throwable cause) {
    super(cause);
  }

  public ReleaseLockException(String message, Throwable cause) {
    super(message, cause);
  }
}
