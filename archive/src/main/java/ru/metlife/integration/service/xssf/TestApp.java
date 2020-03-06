package ru.metlife.integration.service.xssf;


import ru.metlife.integration.service.xssf.XlsService.SheetData;

public class TestApp {

  public static void main(String[] args) {
    XlsService xlsService = new XlsService(
        "C:/Users/Vitalii_Kim/Desktop/winsw/Регистрация_30.08.13.xlsx");
    SheetData sheetData = xlsService
        .processSheet("Справочник", 0, 3, new DictionaryRowContentCallback());
    System.out.println(sheetData);
    System.out.println(sheetData.getData().size());
  }
}
