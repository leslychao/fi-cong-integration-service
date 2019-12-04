package ru.metlife.integration.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.dto.RecipientDto;
import ru.metlife.integration.service.XlsService.SheetData;

@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentExportServiceTest {

  @Mock
  private XlsService xlsService;

  @Mock
  private OrderService orderService;

  @Mock
  private DataFiTimeFreezeService dataFiTimeFreezeService;

  @Mock
  private DictionaryService dictionaryService;

  @InjectMocks
  private DocumentExportService documentExportService;

  private SheetData prepareSheetData() {
    SheetData sheetData = new SheetData();
    List<Map<String, Object>> data = new ArrayList<>();
    sheetData.setData(data);

    Map<String, Object> validOrder = new HashMap<>();
    validOrder.put("order_id", "");
    validOrder.put("delivery_status", "PENDING");
    validOrder.put("№ п/п", "1111");
    validOrder.put("Номер сертификата", "LRM001");
    validOrder.put("ФИО", "Ivanov Ivan");
    validOrder.put("Комментарии", "comment1");
    validOrder.put("Тип документа", "doctype1");
    validOrder.put("Дилерский центр", "dealership1");
    validOrder.put("Партнер", "partner1");
    validOrder.put("Region", "region1");

    Map<String, Object> oneMoreValidOrder = new HashMap<>();
    oneMoreValidOrder.put("order_id", "");
    oneMoreValidOrder.put("delivery_status", "PENDING");
    oneMoreValidOrder.put("№ п/п", "2222");
    oneMoreValidOrder.put("Номер сертификата", "LRM002");
    oneMoreValidOrder.put("ФИО", "Peter Petrov");
    oneMoreValidOrder.put("Комментарии", "comment2");
    oneMoreValidOrder.put("Тип документа", "doctype2");
    oneMoreValidOrder.put("Дилерский центр", "dealership2");
    oneMoreValidOrder.put("Партнер", "partner2");
    oneMoreValidOrder.put("Region", "region2");

    Map<String, Object> orderWithEmptyPolNum = new HashMap<>();
    orderWithEmptyPolNum.put("order_id", "");
    orderWithEmptyPolNum.put("delivery_status", "PENDING");
    orderWithEmptyPolNum.put("№ п/п", "3333");
    orderWithEmptyPolNum.put("Номер сертификата", "");
    orderWithEmptyPolNum.put("ФИО", "Harry Potter");
    orderWithEmptyPolNum.put("Комментарии", "comment3");
    orderWithEmptyPolNum.put("Тип документа", "doctype3");
    orderWithEmptyPolNum.put("Дилерский центр", "dealership3");
    orderWithEmptyPolNum.put("Партнер", "partner3");
    orderWithEmptyPolNum.put("Region", "region3");

    Map<String, Object> orderWithPolNumIsSovkomBank = new HashMap<>();
    orderWithPolNumIsSovkomBank.put("order_id", "");
    orderWithPolNumIsSovkomBank.put("delivery_status", "PENDING");
    orderWithPolNumIsSovkomBank.put("№ п/п", "4444");
    orderWithPolNumIsSovkomBank.put("Номер сертификата", "Совкомбанк");
    orderWithPolNumIsSovkomBank.put("ФИО", "Noname Noname");
    orderWithPolNumIsSovkomBank.put("Комментарии", "comment4");
    orderWithPolNumIsSovkomBank.put("Тип документа", "doctype4");
    orderWithPolNumIsSovkomBank.put("Дилерский центр", "dealership4");
    orderWithPolNumIsSovkomBank.put("Партнер", "partner4");
    orderWithPolNumIsSovkomBank.put("Region", "region4");

    Map<String, Object> orderWithPresentedOrderId = new HashMap<>();
    orderWithPresentedOrderId.put("order_id", "UUID5");
    orderWithPresentedOrderId.put("delivery_status", "PENDING");
    orderWithPresentedOrderId.put("№ п/п", "5555");
    orderWithPresentedOrderId.put("Номер сертификата", "LRM005");
    orderWithPresentedOrderId.put("ФИО", "Jon Snow");
    orderWithPresentedOrderId.put("Комментарии", "comment5");
    orderWithPresentedOrderId.put("Тип документа", "doctype5");
    orderWithPresentedOrderId.put("Дилерский центр", "dealership5");
    orderWithPresentedOrderId.put("Партнер", "partner5");
    orderWithPresentedOrderId.put("Region", "region5");

    data.add(validOrder);
    data.add(oneMoreValidOrder);
    data.add(orderWithEmptyPolNum);
    data.add(orderWithPolNumIsSovkomBank);
    data.add(orderWithPresentedOrderId);
    return sheetData;
  }

  @Test
  public void testGetOrders() {
    List<RecipientDto> recipientDtoList = new ArrayList<>();
    when(dictionaryService
        .getRecipientsFromDictionary(getOrderIndependentHash("dealership1", "partner1", "region1")))
        .thenReturn(Arrays
            .asList(new RecipientDto("ivanov.ivan@email.com", ""),
                new RecipientDto("ivanov1.ivan1@email.com", ""),
                new RecipientDto("ivanov2.ivan2@email.com", "")
            ));

    when(dictionaryService
        .getRecipientsFromDictionary(getOrderIndependentHash("partner2", "region2", "dealership2")))
        .thenReturn(Arrays
            .asList(new RecipientDto("petr_petrov@email.com", ""),
                new RecipientDto("petr1_petrov1@email.com", ""),
                new RecipientDto("petr3_petrov3@email.com", "")
            ));
    List<OrderDto> listOrders = documentExportService.getOrdersToExport(prepareSheetData());
    assertNotNull(listOrders);
    assertThat(listOrders, hasSize(6));

    assertThat(listOrders, hasItems(
        hasProperty("ppNum", is("1111")),
        hasProperty("recipient", is("ivanov.ivan@email.com")),
        hasProperty("recipient", is("ivanov1.ivan1@email.com")),
        hasProperty("recipient", is("ivanov2.ivan2@email.com"))
    ));

    assertThat(listOrders, hasItems(
        hasProperty("ppNum", is("2222")),
        hasProperty("recipient", is("petr_petrov@email.com")),
        hasProperty("recipient", is("petr1_petrov1@email.com")),
        hasProperty("recipient", is("petr3_petrov3@email.com"))
    ));
  }

  @Test
  public void getOrdersWithEmptyPolNum() {
    when(dictionaryService
        .getRecipientsFromDictionary(getOrderIndependentHash("dealership3", "partner3", "region3")))
        .thenReturn(Arrays
            .asList(new RecipientDto("harry.potter@email.com", "")));
    List<OrderDto> listOrders = documentExportService.getOrdersToExport(prepareSheetData());
    assertThat(listOrders, not(hasItem(
        hasProperty("ppNum", is("3333"))
    )));
  }

  @Test
  public void testGetOrdersWithPolNumIsSovkomBank() {
    when(dictionaryService
        .getRecipientsFromDictionary(getOrderIndependentHash("dealership4", "partner4", "region4")))
        .thenReturn(Arrays
            .asList(new RecipientDto("noname_noname@email.com", "")));
    List<OrderDto> listOrders = documentExportService.getOrdersToExport(prepareSheetData());
    assertThat(listOrders, not(hasItems(
        hasProperty("ppNum", is("4444"))
    )));
  }

  @Test
  public void testGetOrdersWithPresentedOrderId() {
    when(dictionaryService
        .getRecipientsFromDictionary(getOrderIndependentHash("dealership5", "partner5", "region5")))
        .thenReturn(Arrays
            .asList(new RecipientDto("jon_snow@email.com", "")));
    List<OrderDto> listOrders = documentExportService.getOrdersToExport(prepareSheetData());
    assertThat(listOrders, not(hasItems(
        hasProperty("ppNum", is("5555"))
    )));
  }

}
