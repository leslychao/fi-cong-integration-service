package ru.metlife.integration.service.xssf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DictionaryRowContentCallback implements ExcelRowContentCollback {

  @Override
  public void processRow(int rowNum, Map<String, String> mapData, List<Map<String, String>> data) {
    data.add(new LinkedHashMap<>(mapData));
  }
}
