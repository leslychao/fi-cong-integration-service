package ru.metlife.integration.service.xssf;

public class WorkbookStoreException extends RuntimeException {

  public WorkbookStoreException() {
  }

  public WorkbookStoreException(String message) {
    super(message);
  }

  public WorkbookStoreException(Throwable cause) {
    super(cause);
  }

  public WorkbookStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
