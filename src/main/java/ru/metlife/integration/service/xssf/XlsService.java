package ru.metlife.integration.service.xssf;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import ru.metlife.integration.exception.ReleaseLockException;

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
  private SheetData sheetData;

  public XlsService(String docFilePath) {
    this(docFilePath, DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS);
  }

  public XlsService(String docFilePath, long lockRepeatIntervalInMillis) {
    this.docFilePath = docFilePath;
    this.lockRepeatIntervalInMillis = lockRepeatIntervalInMillis;
  }

  public void acquireLock() {
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

  public void saveWorkbook(boolean isBackupFile, ProcessBuilder processBuilder) {
    LOGGER.info("saveWorkbook started");
    File file = getFile(docFilePath);
    if (isBackupFile) {
      backupFile(file);
    }
    Process process;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      int exitVal = process.waitFor();
      if (exitVal == 0) {
        LOGGER.info("save workbook performed successfully");
      } else {
        throw new RuntimeException("save workbook fail");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateRow(Map<String, String> data, int rowNum, StringBuilder stringBuilder) {
    data.entrySet()
        .forEach(entry -> updateCell(stringBuilder, entry.getKey(), entry.getValue(), rowNum));
  }

  public void updateCell(StringBuilder stringBuilder, String cellName, String cellValue,
      int rowNum) {
    stringBuilder.append(String.format(
        "objExcel.ActiveSheet.Cells(%d, %d).Value = \"%s\"", rowNum,
        sheetData.getColumnIndex(cellName), cellValue))
        .append("\n");
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
