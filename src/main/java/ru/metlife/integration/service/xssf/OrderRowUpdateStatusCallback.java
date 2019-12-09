package ru.metlife.integration.service.xssf;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;
import static ru.metlife.integration.util.Constants.DELIVERY_STATUS_COMPLETED;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderRowUpdateStatusCallback implements ExcelRowContentCollback {

  @Override
  public void processRow(int rowNum, Map<String, String> mapData, List<Map<String, String>> data) {
    String orderId = getStringCellValue(mapData, "order_id");
    String deliveryStatus = getStringCellValue(mapData, "delivery_status");
    if (!isBlank(orderId) && (!DELIVERY_STATUS_COMPLETED.equals(deliveryStatus))) {
      mapData.put("rowNum", valueOf(rowNum));
      data.add(new LinkedHashMap<>(mapData));
    }
  }

}
