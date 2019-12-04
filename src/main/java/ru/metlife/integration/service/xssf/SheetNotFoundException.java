package ru.metlife.integration.service.xssf;

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
