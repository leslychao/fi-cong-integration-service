package ru.metlife.integration.service;

import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.io.IOUtils.buffer;
import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.STRING;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.metlife.integration.exception.CloseWorkbookException;
import ru.metlife.integration.exception.ReleaseLockException;
import ru.metlife.integration.exception.SheetNotFoundException;
import ru.metlife.integration.exception.WorkbookCreationException;
import ru.metlife.integration.exception.WorkbookStoreException;

/**
 * This class designed to read data from an xls or an xlsx document using Apache POI
 */
public class XlsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XlsService.class);

  private static final long DEFAULT_INITIAL_DELAY_IN_MILLIS = 0;
  private static final long DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS = 5000;

  private String docFilePath;
  private Workbook workbook;
  private ByteArrayOutputStream byteArrayOutputStream;
  private SheetData sheetData;

  private long initialDelayInMillis = DEFAULT_INITIAL_DELAY_IN_MILLIS;
  private long lockRepeatIntervalInMillis;
  private FileLock fileLock;
  private RandomAccessFile randomAccessFile;

  public XlsService(String docFilePath) {
    this.docFilePath = docFilePath;
    this.lockRepeatIntervalInMillis = DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS;
  }

  public XlsService(String docFilePath, long lockRepeatIntervalInMillis) {
    this.docFilePath = docFilePath;
    this.lockRepeatIntervalInMillis = lockRepeatIntervalInMillis;
  }

  /**
   * If lock object is valid then invoking this method releases the lock and releases any resources
   * associated with holding the lock. If lock object is invalid then invoking this method has no
   * effect
   */
  void releaseLock() {
    if (!isNull(fileLock)) {
      try {
        fileLock.release();
        fileLock = null;
      } catch (IOException e) {
        throw new ReleaseLockException(e);
      }
    }
    if (!isNull(randomAccessFile)) {
      try {
        randomAccessFile.close();
        randomAccessFile = null;
      } catch (IOException e) {
        throw new ReleaseLockException(e);
      }
    }
  }

  /**
   * The method tries to get an shared lock using a file as a monitor, if at the moment the file is
   * occupied by another process, the method goes into waiting this creates a separate {@link
   * java.lang.Thread}, which with a given frequency, checks whether file is free, once file is
   * free, it sends a signal
   */
  void acquireLock() {
    ReentrantLock reentrantLock = new ReentrantLock();
    Condition condition = reentrantLock.newCondition();
    ScheduledExecutorService scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor();
    AtomicBoolean atomicBoolean = new AtomicBoolean();
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      try {
        reentrantLock.lock();
        atomicBoolean.set(tryLock());
        if (atomicBoolean.get()) {
          condition.signalAll();
          scheduledExecutorService.shutdown();
        }
      } finally {
        reentrantLock.unlock();
      }
    }, initialDelayInMillis, lockRepeatIntervalInMillis, TimeUnit.MILLISECONDS);
    reentrantLock.lock();
    while (!atomicBoolean.get()) {
      try {
        condition.await();
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage(), e);
        return;
      } finally {
        reentrantLock.unlock();
      }
    }
  }

  boolean tryLock() {
    File file = getFile(docFilePath);
    if (isNull(randomAccessFile)) {
      try {
        randomAccessFile = new RandomAccessFile(file, "rw");
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
        return false;
      }
    }
    FileChannel fileChannel = randomAccessFile.getChannel();
    try {
      fileLock = fileChannel.tryLock(0L, Long.MAX_VALUE, true);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      return false;
    }
    return !isNull(fileLock);
  }

  /**
   * This method parses {@link Sheet}, and generates metadata
   *
   * @param sheetName    the sheet name
   * @param headerRowNum the line number starting with the heading
   * @return {@link SheetData} data with which the application works
   */
  public SheetData processSheet(String sheetName, int headerRowNum) {
    this.sheetData = new SheetData();
    sheetData.setSheetName(sheetName);
    sheetData.setHeaderRowNum(headerRowNum);
    Sheet sheet = getSheet(sheetName);
    sheetData.setSheet(sheet);
    int rowCount = sheet.getLastRowNum() + 1;
    sheetData.setRowCount(rowCount);
    int lastRowNum = sheet.getLastRowNum();
    sheetData.setLastRowNum(lastRowNum);
    int startRowNum = sheet.getFirstRowNum();
    for (int rowNum = headerRowNum; rowNum < rowCount; rowNum++) {
      Row row = sheet.getRow(rowNum);
      if (!isRowEmpty(row)) {
        startRowNum = rowNum + 1;
        sheetData.setStartRowNum(startRowNum);
        break;
      }
    }
    Map<String, Integer> columnIndex = buildColumnIndex(sheet, headerRowNum);
    sheetData.setColumnIndex(columnIndex);
    List<Map<String, Object>> data = new ArrayList<>();
    for (int rowNum = startRowNum; rowNum < lastRowNum + 1; rowNum++) {
      Row row = sheet.getRow(rowNum);
      if (isRowEmpty(row)) {
        continue;
      }
      Map<String, Object> targetMap = new LinkedHashMap<>();
      for (Map.Entry<String, Integer> entry : columnIndex.entrySet()) {
        {
          targetMap.put(entry.getKey(), readCell(row, entry.getValue()));
        }
      }
      data.add(targetMap);
    }
    sheetData.setData(data);
    return sheetData;
  }

  /**
   * Constructs a Workbook object
   *
   * @param isLocked denotes whether file should be locked
   */
  public void openWorkbook(boolean isLocked) {
    if (isLocked) {
      acquireLock();
    }
    this.workbook = getOrLoadWorkbook();
    this.byteArrayOutputStream = new ByteArrayOutputStream();
  }


  /**
   * Closes {@code Workbook} and releases all captured locks
   *
   * @see Workbook#close()
   * @see #releaseLock()
   */
  public void closeWorkbook() {
    if (!isNull(workbook)) {
      try (OutputStream outputStream = buffer(new FileOutputStream(docFilePath))) {
        workbook.close();
        releaseLock();
        if (byteArrayOutputStream.size() > 0) {
          outputStream.write(byteArrayOutputStream.toByteArray());
          byteArrayOutputStream.close();
          byteArrayOutputStream = new ByteArrayOutputStream();
        }
      } catch (IOException e) {
        throw new CloseWorkbookException(e);
      }
      this.workbook = null;
      this.sheetData = null;
      LOGGER.info("{} written successfully on disk", docFilePath);
    }
  }


  /**
   * This method Obtains the current Workbook or creates new one from an Excel file
   *
   * @return The created {@link Workbook}, never returns {@code null}
   * @throws WorkbookCreationException if an error occurs
   */
  Workbook getOrLoadWorkbook() {
    if (!isNull(workbook)) {
      return workbook;
    }
    ZipSecureFile.setMinInflateRatio(0);
    File file = getFile(docFilePath);
    Workbook workbook;
    try (InputStream inputStream = buffer(new FileInputStream(file))) {
      workbook = new XSSFWorkbook(inputStream);
    } catch (IOException e) {
      throw new WorkbookCreationException(e);
    }
    return workbook;
  }

  /**
   * The method creates a backup copy of the file with in the same directory
   *
   * @param file for which you want to create a backup
   */
  void backupFile(File file) {
    LOGGER.info("creating file backup for {}", file.getAbsolutePath());
    File backupFile = getFile(docFilePath + ".bkp");
    try {
      deleteQuietly(backupFile);
      try (FileInputStream fileInputStream = new FileInputStream(
          file); FileOutputStream fileOutputStream = new FileOutputStream(backupFile);) {
        IOUtils.copy(fileInputStream, fileOutputStream);
      }
    } catch (IOException ex) {
      throw new RuntimeException("Unable to create backup file " + backupFile.getAbsolutePath());
    }
    LOGGER.info("{} successfully written on disk", backupFile.getAbsolutePath());
  }

  /**
   * Writes {@link Workbook} data to a file
   *
   * @param isBackupFile if true, creates a backup for file denoted by {@code docFilePath}
   */
  public void saveWorkbook(boolean isBackupFile) {
    File file = getFile(docFilePath);
    if (isBackupFile) {
      backupFile(file);
    }
    try {
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      throw new WorkbookStoreException(e);
    }
  }


  Sheet getSheet(String sheetName) {
    Iterator<Sheet> sheetIterator = getOrLoadWorkbook().sheetIterator();
    while (sheetIterator.hasNext()) {
      Sheet sheet = sheetIterator.next();
      if (sheet.getSheetName().contains(sheetName)) {
        return sheet;
      }
    }
    throw new SheetNotFoundException("Could not find sheet " + sheetName);
  }

  Map<String, Integer> buildColumnIndex(Sheet sheet, int headerRowNum) {
    Map<String, Integer> columnIndex = new LinkedHashMap<>();
    Row row = sheet.getRow(headerRowNum);
    if (isRowEmpty(row)) {
      throw new IllegalArgumentException("Can't find header row headerRowNum " + headerRowNum);
    }
    row.cellIterator().forEachRemaining(cell -> {
      if (!isCellEmpty(cell)) {
        columnIndex.put(cell.toString(), cell.getColumnIndex());
      }
    });
    return columnIndex;
  }

  boolean isCellEmpty(Cell cell) {
    return isNull(cell) || BLANK == cell.getCellType();
  }

  boolean isRowEmpty(Row row) {
    return isNull(row) || row.getLastCellNum() <= 0;
  }

  public void addRows(List<Map<String, Object>> data, int startRowNum) {
    AtomicInteger atomicInteger = new AtomicInteger(startRowNum);
    data.forEach(
        stringObjectMap -> addRow(stringObjectMap, atomicInteger.getAndIncrement()));
  }

  public void addRow(Map<String, Object> data, int rowNum) {
    Sheet sheet = sheetData.getSheet();
    if (isRowEmpty(sheet.getRow(rowNum))) {
      sheet.createRow(rowNum);
      sheetData.setLastRowNum(rowNum);
      sheetData.setRowCount(rowNum + 1);
    }
    data.forEach((cellName, cellValue) -> {
      int cellIndex = sheetData.getColumnIndex(cellName);
      setCellValue(rowNum, cellIndex, cellValue);
    });
  }

  public void updateRow(Map<String, Object> data,
      Predicate<Row> predicate) {
    data.entrySet().forEach(
        entry -> updateCell(entry.getKey(), entry.getValue(), predicate)
    );
  }

  /**
   * Set a string value for the {@code cell}. Selects only {@code rows} for which the predicate
   * expression evaluates to {@code true}
   *
   * @param cellName  - the cell name
   * @param cellValue - the ell value
   * @param predicate - the predicate, defines which rows to accept
   */
  public void updateCell(String cellName, Object cellValue,
      Predicate<Row> predicate) {
    Map<String, Integer> columnIndex = sheetData.getColumnIndex();
    Sheet sheet = sheetData.getSheet();
    int startRowNum = sheetData.getStartRowNum();
    int rowCount = sheetData.getRowCount();
    int headerRowNum = sheetData.getHeaderRowNum();
    Row headerRow = sheet.getRow(headerRowNum);
    int cellIndex = sheetData.getColumnIndex(cellName);
    if (cellIndex < 0) {
      LOGGER.warn("updateCell: Cell with name {} not found, creating...", cellName);
      Cell headerCell = headerRow.createCell(headerRow.getLastCellNum());
      cellIndex = headerCell.getColumnIndex();
      headerCell.setCellValue(cellName);
      columnIndex.put(headerCell.toString(), cellIndex);
      LOGGER.warn("updateCell: Cell with name {} successfully created", cellName);
    }
    for (int rowNum = startRowNum; rowNum < rowCount; rowNum++) {
      Row row = sheet.getRow(rowNum);
      if (isRowEmpty(row)) {
        continue;
      }
      if (predicate.test(row)) {
        setCellValue(rowNum, cellIndex, cellValue);
      }
    }
  }

  void setCellValue(int rowNum, int cellIndex, Object cellValue) {
    Sheet sheet = sheetData.getSheet();
    Row row = sheet.getRow(rowNum);
    if (isRowEmpty(row)) {
      row = sheet.createRow(rowNum);
    }
    Cell cell = row.getCell(cellIndex);
    if (isCellEmpty(cell)) {
      cell = row.createCell(cellIndex, STRING);
    }
    cell.setCellValue(valueOf(cellValue));
  }

  Object readNumericCell(Cell cell) {
    if (DateUtil.isCellDateFormatted(cell)) {
      return cell.getDateCellValue();
    }
    BigDecimal numericCellValue = BigDecimal.valueOf(cell.getNumericCellValue());
    if (numericCellValue.stripTrailingZeros().scale() <= 0) {
      return numericCellValue.longValue();
    }
    return numericCellValue.doubleValue();
  }

  Object readFormulaCell(Cell cell) {
    switch (cell.getCachedFormulaResultType()) {
      case NUMERIC:
        return readNumericCell(cell);
      case STRING:
        return cell.getRichStringCellValue().getString();
      default:
        return cell.toString();
    }
  }

  Object readCell(Row row, int cellIdx) {
    return readCell(row.getCell(cellIdx));
  }

  Object readCell(Cell cell) {
    if (isCellEmpty(cell)) {
      return null;
    }
    switch (cell.getCellType()) {
      case NUMERIC:
        return readNumericCell(cell);
      case BOOLEAN:
        return cell.getBooleanCellValue();
      case FORMULA:
        return readFormulaCell(cell);
      case STRING:
        return cell.getStringCellValue();
      default:
        return cell.toString();
    }
  }

  @Getter
  @Setter
  @ToString
  public static class SheetData {

    private String sheetName;
    private Sheet sheet;
    private int headerRowNum;
    private int startRowNum;
    private int lastRowNum;
    private int rowCount;
    private List<Map<String, Object>> data = new ArrayList<>();
    private Map<String, Integer> columnIndex = new HashMap<>();

    public int getColumnIndex(String columnName) {
      return ofNullable(columnIndex.get(columnName))
          .orElseGet(() -> {
            LOGGER.warn("Can't find index for column {}", columnName);
            return -1;
          });
    }
  }
}
