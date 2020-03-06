package ru.metlife.integration.exception;

public class CloseWorkbookException extends RuntimeException {

  public CloseWorkbookException() {
  }

  public CloseWorkbookException(String message) {
    super(message);
  }

  public CloseWorkbookException(Throwable cause) {
    super(cause);
  }

  public CloseWorkbookException(String message, Throwable cause) {
    super(message, cause);
  }
}
