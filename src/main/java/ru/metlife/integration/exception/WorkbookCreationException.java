package ru.metlife.integration.exception;

public class WorkbookCreationException extends RuntimeException {

  public WorkbookCreationException() {
  }

  public WorkbookCreationException(String message) {
    super(message);
  }

  public WorkbookCreationException(Throwable cause) {
    super(cause);
  }

  public WorkbookCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
