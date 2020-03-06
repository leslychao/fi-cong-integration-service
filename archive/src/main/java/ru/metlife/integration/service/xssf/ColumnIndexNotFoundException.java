package ru.metlife.integration.service.xssf;

public class ColumnIndexNotFoundException extends RuntimeException {

  public ColumnIndexNotFoundException() {
  }

  public ColumnIndexNotFoundException(String message) {
    super(message);
  }

  public ColumnIndexNotFoundException(Throwable cause) {
    super(cause);
  }

  public ColumnIndexNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
