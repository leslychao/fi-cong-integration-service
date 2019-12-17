package ru.metlife.integration.service.xssf;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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

public class XlsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      XlsService.class);

  private String docFilePath;
  private SheetData sheetData;

  public XlsService(String docFilePath) {
    this.docFilePath = docFilePath;
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
