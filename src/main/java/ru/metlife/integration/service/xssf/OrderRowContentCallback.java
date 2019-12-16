package ru.metlife.integration.service.xssf;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    String polNum = getStringCellValue(mapData, "Номер сертификата");
    String orderId = getStringCellValue(mapData, "order_id");
    String dealership = getStringCellValue(mapData, "Дилерский центр");
    String partner = getStringCellValue(mapData, "Партнер");
    String region = getStringCellValue(mapData, "Region");
    List<RecipientDto> recipients = dictionaryService
        .getRecipientsFromDictionary(dictionarySheetData,
            getOrderIndependentHash(dealership, partner, region));
    if (!recipients.isEmpty()
        && isBlank(orderId)
        && isNotBlank(polNum)
        && !Objects.equals("Совкомбанк", polNum)) {
      mapData.put("rowNum", valueOf(rowNum));
      data.add(new LinkedHashMap<>(mapData));
    }
  }
}
