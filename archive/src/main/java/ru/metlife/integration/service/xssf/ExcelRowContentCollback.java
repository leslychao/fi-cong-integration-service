package ru.metlife.integration.service.xssf;

import java.util.List;
import java.util.Map;

public interface ExcelRowContentCollback {

  void processRow(int rowNum, Map<String, String> mapData, List<Map<String, String>> data);

}
