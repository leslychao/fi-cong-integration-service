package ru.metlife.integration.service.xssf;

import ru.metlife.integration.service.XlsService.SheetData;

public class TestApp {

  public static void main(String[] args) {
    XssfXlsService xlsService = new XssfXlsService(
        "C:/Users/Vitalii_Kim/Desktop/winsw/Регистрация_30.08.13.xlsx");
    SheetData sheetData = xlsService.processSheet("Общая", 0, 0);
    System.out.println(sheetData);
  }
}
