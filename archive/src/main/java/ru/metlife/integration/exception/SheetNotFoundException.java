package ru.metlife.integration.exception;

public class SheetNotFoundException extends RuntimeException {

  public SheetNotFoundException() {
  }

  public SheetNotFoundException(String message) {
    super(message);
  }

  public SheetNotFoundException(Throwable cause) {
    super(cause);
  }

  public SheetNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
