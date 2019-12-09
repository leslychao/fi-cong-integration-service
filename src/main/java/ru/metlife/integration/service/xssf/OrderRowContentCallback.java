package ru.metlife.integration.service.xssf;

import static java.lang.String.valueOf;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.metlife.integration.dto.RecipientDto;
import ru.metlife.integration.service.DictionaryService;
import ru.metlife.integration.service.xssf.XlsService.SheetData;

public class OrderRowContentCallback implements ExcelRowContentCollback {

  private DictionaryService dictionaryService;
  private SheetData dictionarySheetData;

  public OrderRowContentCallback(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
    dictionarySheetData = dictionaryService.processSheet();
  }

  @Override
  public void processRow(int rowNum, Map<String, String> mapData, List<Map<String, String>> data) {
    List<RecipientDto> recipients = dictionaryService
        .getRecipientsFromDictionary(dictionarySheetData,
            getOrderIndependentHash(getStringCellValue(mapData, "Дилерский центр"),
                getStringCellValue(mapData, "Партнер"),
                getStringCellValue(mapData, "Region")));
    if (!recipients.isEmpty()) {
      mapData.put("rowNum", valueOf(rowNum));
      data.add(new LinkedHashMap<>(mapData));
    }
  }

}
