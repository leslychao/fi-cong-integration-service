package ru.metlife.integration.service.xssf;

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
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import ru.metlife.integration.exception.CloseWorkbookException;
import ru.metlife.integration.exception.ReleaseLockException;
import ru.metlife.integration.exception.SheetNotFoundException;
import ru.metlife.integration.exception.WorkbookCreationException;
import ru.metlife.integration.exception.WorkbookStoreException;

public class XlsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      XlsService.class);

  private static final long DEFAULT_INITIAL_DELAY_IN_MILLIS = 0;
  private static final long DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS = 5000;
  private static String BACKUP_FILE_EXTENSION = ".bkp";

  private long initialDelayInMillis = DEFAULT_INITIAL_DELAY_IN_MILLIS;
  private long lockRepeatIntervalInMillis;

  private FileLock fileLock;
  private RandomAccessFile randomAccessFile;

  private String docFilePath;
  private Workbook workbook;

  private ByteArrayOutputStream byteArrayOutputStream;
  private SheetData sheetData;

  public XlsService(String docFilePath) {
    this(docFilePath, DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS);
  }

  public XlsService(String docFilePath, long lockRepeatIntervalInMillis) {
    this.docFilePath = docFilePath;
    this.lockRepeatIntervalInMillis = lockRepeatIntervalInMillis;
  }

  public void releaseLock() {
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

  public void openWorkbook(boolean isLocked) {
    if (isLocked) {
      acquireLock();
    }
    this.workbook = getOrLoadWorkbook();
    this.byteArrayOutputStream = new ByteArrayOutputStream();
  }

  public void closeWorkbook() {
    if (!isNull(workbook)) {
      try (OutputStream outputStream = buffer(new FileOutputStream(docFilePath))) {
        workbook.close();
        releaseLock();
        if (byteArrayOutputStream.size() > 0) {
          outputStream.write(byteArrayOutputStream.toByteArray());
        }
      } catch (IOException e) {
        throw new CloseWorkbookException(e);
      } finally {
        try {
          byteArrayOutputStream.close();
        } catch (IOException e) {
        }
        byteArrayOutputStream = new ByteArrayOutputStream();
      }
      this.workbook = null;
      this.sheetData = null;
      LOGGER.info("{} written successfully on disk", docFilePath);
    }
  }

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

  void backupFile(File file) {
    LOGGER.info("creating file backup for {}", file.getAbsolutePath());
    File backupFile = getFile(docFilePath + BACKUP_FILE_EXTENSION);
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

  boolean isCellEmpty(Cell cell) {
    return isNull(cell) || BLANK == cell.getCellType();
  }

  boolean isRowEmpty(Row row) {
    return isNull(row) || row.getLastCellNum() <= 0;
  }

  public void addRows(List<Map<String, String>> data, int startRowNum) {
    AtomicInteger atomicInteger = new AtomicInteger(startRowNum);
    data.forEach(
        stringObjectMap -> addRow(stringObjectMap, atomicInteger.getAndIncrement()));
  }

  public void addRow(Map<String, String> data, int rowNum) {
    Sheet sheet = getSheet(sheetData.getSheetName());
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

  public void updateRow(Map<String, String> data, int rowNum) {
    data.entrySet().forEach(
        entry -> updateCell(entry.getKey(), entry.getValue(), rowNum)
    );
  }

  public void updateCell(String cellName, String cellValue, int rowNum) {
    Map<String, Integer> columnIndex = sheetData.getColumnIndex();
    Sheet sheet = getSheet(sheetData.getSheetName());
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
    Row row = sheet.getRow(rowNum);
    if (isRowEmpty(row)) {
      return;
    }
    setCellValue(rowNum, cellIndex, cellValue);
  }

  void setCellValue(int rowNum, int cellIndex, String cellValue) {
    Sheet sheet = getSheet(sheetData.getSheetName());
    Row row = sheet.getRow(rowNum);
    if (isRowEmpty(row)) {
      row = sheet.createRow(rowNum);
    }
    Cell cell = row.getCell(cellIndex);
    if (isCellEmpty(cell)) {
      cell = row.createCell(cellIndex, STRING);
    }
    cell.setCellValue(cellValue);
  }

  public SheetData processSheet(String sheetName, int skipRowNum,
      int headerRowNum, ExcelRowContentCollback excelRowContentCollback) {
    sheetData = new SheetData();
    sheetData.setSheetName(sheetName);
    sheetData.setHeaderRowNum(headerRowNum);
    try {
      OPCPackage opcPackage = OPCPackage.open(docFilePath);
      XSSFReader xssfReader = new XSSFReader(opcPackage);
      XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader
          .getSheetsData();
      while (sheetIterator.hasNext()) {
        InputStream inputStream = sheetIterator.next();
        if (sheetName.equals(sheetIterator.getSheetName())) {
          try {
            processSheet(
                xssfReader.getStylesTable(),
                new ReadOnlySharedStringsTable(opcPackage),
                new ExcelWorkSheetHandler(excelRowContentCollback, skipRowNum, headerRowNum),
                inputStream);
          } finally {
            inputStream.close();
          }
        }
      }
      return sheetData;
    } catch (IOException | SAXException | OpenXML4JException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  void processSheet(StylesTable stylesTable, ReadOnlySharedStringsTable readOnlySharedStringsTable,
      SheetContentsHandler sheetContentsHandler,
      InputStream inputStream) throws IOException, ParserConfigurationException, SAXException {
    try {
      XMLReader xmlReader = SAXHelper.newXMLReader();
      ContentHandler xssfSheetXMLHandler = new XSSFSheetXMLHandler(
          stylesTable,
          null,
          readOnlySharedStringsTable,
          sheetContentsHandler,
          new DataFormatter(),
          false);
      xmlReader.setContentHandler(xssfSheetXMLHandler);
      xmlReader.parse(new InputSource(inputStream));
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private class ExcelWorkSheetHandler implements SheetContentsHandler {

    ExcelRowContentCollback excelRowContentCollback;
    private Map<String, String> rowTmp = new LinkedHashMap<>();
    private Map<Integer, String> cellMapping = new HashMap<>();

    private int currentRowNum;
    private int skipRowNum;
    private int headerRowNum;
    private boolean isFirstRow = true;

    ExcelWorkSheetHandler(
        ExcelRowContentCollback excelRowContentCollback, int skipRowNum, int headerRowNum) {
      this.excelRowContentCollback = excelRowContentCollback;
      this.skipRowNum = skipRowNum;
      this.headerRowNum = headerRowNum;
    }

    int getColumnIndex(String cellReference) {
      return (new CellReference(cellReference)).getCol();
    }

    @Override
    public void startRow(int rowNum) {
      currentRowNum = rowNum;
    }

    @Override
    public void endRow(int rowNum) {
      if (currentRowNum >= skipRowNum && currentRowNum != headerRowNum) {
        if (!rowTmp.isEmpty()) {
          if (isFirstRow) {
            isFirstRow = false;
            sheetData.setStartRowNum(rowNum);
          }
          excelRowContentCollback.processRow(rowNum, rowTmp, sheetData.getData());
          rowTmp.clear();
        }
      }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
      if (currentRowNum >= skipRowNum) {
        int idx = getColumnIndex(cellReference);
        if (headerRowNum == currentRowNum) {
          cellMapping.put(idx, formattedValue);
          sheetData.getColumnIndex().put(formattedValue, idx);
        } else {
          rowTmp.put(cellMapping.get(idx), formattedValue);
        }
      }
    }

    @Override
    public void endSheet() {
      sheetData.setLastRowNum(currentRowNum);
      sheetData.setRowCount(currentRowNum + 1);
    }
  }

  @Getter
  @Setter
  @ToString
  public static class SheetData {

    private String sheetName;
    private int headerRowNum;
    private int startRowNum;
    private int lastRowNum;
    private int rowCount;
    private List<Map<String, String>> data = new ArrayList<>();
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
