package ru.metlife.integration.service;


import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.DigestUtils.md5Digest;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;
import static ru.metlife.integration.util.Constants.FI_LETTER_STATUS;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.dto.RecipientDto;
import ru.metlife.integration.service.xssf.OrderRowContentCallback;
import ru.metlife.integration.service.xssf.OrderRowUpdateStatusCallback;
import ru.metlife.integration.service.xssf.XlsService;
import ru.metlife.integration.service.xssf.XlsService.SheetData;


@Service
@Slf4j
public class DocumentExportService {

  @Value("${fi-cong-integration.sender-email}")
  private String sender;

  @Value("${fi-cong-integration.channel}")
  private String channel;

  @Value("${fi-cong-integration.docs-file-path}")
  private String docFilePath;

  @Value("${fi-cong-integration.lock-repeat-interval-in-millis}")
  private long lockRepeatIntervalInMillis;

  private XlsService xlsService;
  private OrderService orderService;
  private DataFiTimeFreezeService dataFiTimeFreezeService;
  private DictionaryService dictionaryService;

  @Autowired
  public DocumentExportService(OrderService orderService,
      DataFiTimeFreezeService dataFiTimeFreezeService,
      DictionaryService dictionaryService) {
    this.orderService = orderService;
    this.dataFiTimeFreezeService = dataFiTimeFreezeService;
    this.dictionaryService = dictionaryService;
  }

  @PostConstruct
  public void init() {
    xlsService = new XlsService(docFilePath, lockRepeatIntervalInMillis);
  }

  void createOrder(List<OrderDto> listOrders) {
    Objects.requireNonNull(listOrders);
    listOrders.forEach(orderDto -> {
      orderService.saveOrder(orderDto);
      dataFiTimeFreezeService.saveOrder(orderDto);
    });
  }

  List<OrderDto> getOrdersToExport(SheetData sheetData) {
    SheetData dictionarySheetData = dictionaryService.processSheet();
    return toOrderDto(sheetData)
        .stream()
        .filter(o -> isBlank(o.getOrderId())
            && isNotBlank(o.getPolNum())
            && !Objects.equals("Совкомбанк", o.getPolNum()))
        .flatMap(o -> {
          List<RecipientDto> recipients = dictionaryService
              .getRecipientsFromDictionary(dictionarySheetData,
                  getOrderIndependentHash(o.getDealership(), o.getPartner(), o.getRegion()));
          return recipients.stream()
              .map(r -> {
                OrderDto newOrder = SerializationUtils.clone(o);
                newOrder.setRecipient(r.getEmail());
                newOrder.setEmailCC(r.getEmailCC());
                return newOrder;
              });
        })
        .collect(toList());
  }

  @Scheduled(cron = "${fi-cong-integration.export-order-cron}")
  @Transactional
  public void exportDocument() {
    log.info("start exportDocument");
    try {
      XlsService.SheetData sheetData = xlsService
          .processSheet("Общая", 0, 0, new OrderRowContentCallback(dictionaryService));
      List<OrderDto> listOrders = getOrdersToExport(sheetData);
      createOrder(listOrders);
      xlsService.openWorkbook(true);
      listOrders.forEach(orderDto -> {
        Map<String, String> toUpdate = new HashMap<>();
        toUpdate.put("order_id", orderDto.getOrderId());
        toUpdate.put("e-mail", orderDto.getRecipient());
        toUpdate.put("e-mail копия", orderDto.getEmailCC());
        xlsService.updateRow(toUpdate, orderDto.getRowNum());
      });
      if (!listOrders.isEmpty()) {
        xlsService.saveWorkbook(true);
      }
    } catch (RuntimeException e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      log.error(e.getMessage());
    } finally {
      xlsService.closeWorkbook();
    }
  }

  @Scheduled(cron = "${fi-cong-integration.update-state-in-docs-file-cron}")
  @Transactional
  public void updateDeliveryStatusInDocsFile() {
    log.info("start updateDeliveryStatusInDocsFile");
    try {
      xlsService.openWorkbook(true);
      XlsService.SheetData sheetData = xlsService
          .processSheet("Общая", 0, 0, new OrderRowUpdateStatusCallback());
      List<OrderDto> listOrdersFromXls = toOrderDto(sheetData);
      listOrdersFromXls.forEach(orderDto -> {
        OrderDto orderDtoFromDb = orderService.findByOrderId(orderDto.getOrderId());
        xlsService.updateCell("delivery_status", orderDtoFromDb.getDeliveryStatus(),
            orderDto.getRowNum());
      });
      xlsService.saveWorkbook(true);
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    } finally {
      xlsService.closeWorkbook();
    }
  }

  List<OrderDto> toOrderDto(XlsService.SheetData sheetData) {
    return ofNullable(sheetData.getData())
        .orElse(emptyList())
        .stream()
        .map(this::toOrderDto)
        .collect(toList());
  }

  OrderDto toOrderDto(Map<String, String> orderFromXls) {
    String orderId = getStringCellValue(orderFromXls, "order_id", null);
    String deliveryStatus = getStringCellValue(orderFromXls, "delivery_status");
    String ppNum = getStringCellValue(orderFromXls, "№ п/п");
    String polNum = getStringCellValue(orderFromXls, "Номер сертификата");
    String clientFio = getStringCellValue(orderFromXls, "ФИО");
    String comments = getStringCellValue(orderFromXls, "Комментарии");
    String docType = getStringCellValue(orderFromXls, "Тип документа");
    String email = getStringCellValue(orderFromXls, "e-mail");
    String emailCC = getStringCellValue(orderFromXls, "e-mail копия");
    String dealership = getStringCellValue(orderFromXls, "Дилерский центр");
    String partner = getStringCellValue(orderFromXls, "Партнер");
    String region = getStringCellValue(orderFromXls, "Region");
    String rowNum = getStringCellValue(orderFromXls, "rowNum");

    OrderDto orderDto = new OrderDto();
    orderDto.setRowNum(Integer.valueOf(rowNum));
    orderDto.setPpNum(ppNum);
    orderDto.setOrderId(orderId);
    orderDto.setDeliveryStatus(deliveryStatus);
    orderDto.setRecipient(email);
    orderDto.setChannel(channel);
    orderDto.setSender(sender);
    String subject = of(dealership, region, polNum, clientFio, docType)
        .filter(StringUtils::isNotBlank)
        .collect(joining("_"));
    orderDto.setSubject(subject);
    orderDto.setStatus(FI_LETTER_STATUS);
    orderDto.setCreatedAt(new Date());
    orderDto.setCreatedBy(sender);
    orderDto.setClientFio(clientFio);
    orderDto.setPolNum(polNum);
    orderDto.setComment(comments);
    orderDto.setDocType(docType);
    orderDto.setEmailCC(emailCC);
    try {
      byte[] entryHash = md5Digest(join("_", subject, email).getBytes("UTF-8"));
      orderDto.setEntryHash(entryHash);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    orderDto.setDealership(dealership);
    orderDto.setPartner(partner);
    orderDto.setRegion(region);
    return orderDto;
  }
}
