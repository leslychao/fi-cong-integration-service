package ru.metlife.integration.service.xssf;

import static java.lang.String.valueOf;
import static ru.metlife.integration.service.xssf.TestApp.prepareRegions;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class TestRowContentCallback implements ExcelRowContentCollback {

  @Override
  public void processRow(int rowNum, Map<String, String> mapData, List<Map<String, String>> data) {
    mapData.put("rowNum", valueOf(rowNum));

    String address = getStringCellValue(mapData, "Адрес_место_нахождения");
    Map<String, String> cladrIds = prepareRegions();
    if (!StringUtils.isEmpty(address)) {
      Pattern p = Pattern.compile("(\\d+,\\s?)([а-яА-Я\\s-]+)(\\.?)(,\\s?)(.*)");
      Matcher m = p.matcher(address);
      if (m.find()) {
        String s = m.group(2);

        String cladrId = cladrIds.get(s);
        if (cladrId == null || cladrId.isEmpty()) {
          System.out.println(s);
        } else {
          mapData.put("cladrId", cladrId);
        }
      }
    }

    data.add(new LinkedHashMap<>(mapData));


  }
}
