package ru.metlife.integration.service.xssf;

import static java.util.Objects.isNull;
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
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
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
import ru.metlife.integration.service.XlsService.SheetData;

public class XssfXlsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XssfXlsService.class);

  private static final long DEFAULT_INITIAL_DELAY_IN_MILLIS = 0;
  private static final long DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS = 5000;
  private static String BACKUP_FILE_EXTENSION = ".bkp";

  private long initialDelayInMillis = DEFAULT_INITIAL_DELAY_IN_MILLIS;
  private long lockRepeatIntervalInMillis = DEFAULT_LOCK_REPEAT_INTERVAL_IN_MILLIS;

  private FileLock fileLock;
  private RandomAccessFile randomAccessFile;

  private String docFilePath;

  private Map<Integer, String> cellMapping = new HashMap<>();
  private Map<String, Integer> columnIndex = new HashMap<>();
  private List<Map<String, String>> data = new ArrayList<>();

  public XssfXlsService(String docFilePath) {
    this.docFilePath = docFilePath;
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

  public SheetData processSheet(String sheetName, int skipRowNum,
      int headerRowNum) {
    try {
      OPCPackage opcPackage = OPCPackage
          .open(new File(docFilePath));
      ReadOnlySharedStringsTable readOnlySharedStringsTable = new ReadOnlySharedStringsTable(
          opcPackage);
      XSSFReader xssfReader = new XSSFReader(opcPackage);
      StylesTable stylesTable = xssfReader.getStylesTable();
      XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader
          .getSheetsData();
      while (sheetIterator.hasNext()) {
        InputStream inputStream = sheetIterator.next();
        if (sheetName.equals(sheetIterator.getSheetName())) {
          processSheet(stylesTable, readOnlySharedStringsTable,
              new ExcelWorkSheetHandler(skipRowNum, headerRowNum), inputStream);
        }
        inputStream.close();
      }
      SheetData sheetData = new SheetData();
      sheetData.setColumnIndex(columnIndex);
      sheetData.setData(data);
      return sheetData;
    } catch (IOException | SAXException | OpenXML4JException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  void processSheet(StylesTable stylesTable, ReadOnlySharedStringsTable readOnlySharedStringsTable,
      SheetContentsHandler sheetContentsHandler,
      InputStream inputStream) throws IOException, ParserConfigurationException, SAXException {
    DataFormatter dataFormatter = new DataFormatter();
    InputSource inputSource = new InputSource(inputStream);
    try {
      XMLReader xmlReader = SAXHelper.newXMLReader();
      ContentHandler xssfSheetXMLHandler = new XSSFSheetXMLHandler(stylesTable, null,
          readOnlySharedStringsTable, sheetContentsHandler,
          dataFormatter, false);
      xmlReader.setContentHandler(xssfSheetXMLHandler);
      xmlReader.parse(inputSource);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  void backupFile(File file) {
    LOGGER.info("creating backup file {}", file);
    File backupFile = getFile(docFilePath, BACKUP_FILE_EXTENSION);
    try {
      deleteQuietly(backupFile);
      try (FileInputStream fileInputStream = new FileInputStream(
          file); FileOutputStream fileOutputStream = new FileOutputStream(backupFile);) {
        IOUtils.copy(fileInputStream, fileOutputStream);
      }
    } catch (IOException ex) {
      throw new RuntimeException("unable to create backup file " + backupFile);
    }
    LOGGER.info("backup file successfully created {}", backupFile);
  }

  private class ExcelWorkSheetHandler implements SheetContentsHandler {
    private Map<String, String> rowTmp = new LinkedHashMap<>();

    private int currentRowNum;
    private int skipRowNum;
    private int headerRowNum;

    public ExcelWorkSheetHandler(int skipRowNum, int headerRowNum) {
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
          data.add(rowTmp);
        }
      }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
      if (currentRowNum >= skipRowNum) {
        int idx = getColumnIndex(cellReference);
        if (headerRowNum == currentRowNum) {
          cellMapping.put(idx, formattedValue);
          columnIndex.put(formattedValue, idx);
        } else {
          rowTmp.put(cellMapping.get(idx), formattedValue);
        }
      }
    }

  }
}
