package ru.metlife.integration.service;

import static java.util.stream.Collectors.toList;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;

import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.dto.DeliveryDataDto;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.dto.RecipientDto;
import ru.metlife.integration.service.xssf.OrderRowContentCallback;
import ru.metlife.integration.service.xssf.XlsService;
import ru.metlife.integration.service.xssf.XlsService.SheetData;


@Service
@Slf4j
public class DocumentExportService {

  @Value("${fi-cong-integration.doc-file-path}")
  private String docFilePath;

  private XlsService xlsService;

  private OrderService orderService;
  private DataFiTimeFreezeService dataFiTimeFreezeService;
  private DictionaryService dictionaryService;
  private DeliveryDataService deliveryDataService;

  @Autowired
  public DocumentExportService(OrderService orderService,
      DataFiTimeFreezeService dataFiTimeFreezeService,
      DictionaryService dictionaryService,
      DeliveryDataService deliveryDataService) {
    this.orderService = orderService;
    this.dataFiTimeFreezeService = dataFiTimeFreezeService;
    this.dictionaryService = dictionaryService;
    this.deliveryDataService = deliveryDataService;
  }

  @PostConstruct
  public void init() {
    xlsService = new XlsService(docFilePath);
  }

  List<OrderDto> getOrdersToExport(SheetData sheetData) {
    SheetData dictionarySheetData = dictionaryService.processSheet();
    return orderService.toOrderDto(sheetData)
        .stream()
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
      SheetData sheetData = xlsService
          .processSheet("Общая", 0, 0,
              new OrderRowContentCallback(dictionaryService, deliveryDataService));
      List<OrderDto> listOrders = getOrdersToExport(sheetData);
      if (!listOrders.isEmpty()) {
        log.info("Orders to export {}", listOrders.size());
        listOrders.forEach(orderDto -> {
          orderService.saveOrder(orderDto);
          dataFiTimeFreezeService.saveOrder(orderDto);
          deliveryDataService.saveDeliveryData(deliveryDataService.toDeliveryDataDto(orderDto));
        });
        log.info("document export completed!");
      } else {
        log.info("exportDocument: Nothing to export");
      }
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
  }

  @Scheduled(cron = "${fi-cong-integration.update-delivery-status-cron}")
  @Transactional
  public void updateDeliveryStatus() {
    log.info("start updateDeliveryStatus");
    try {
      List<DeliveryDataDto> listDeliveryStatuses = deliveryDataService
          .findByDeliveryStatus();
      List<OrderDto> listOrders = listDeliveryStatuses.stream()
          .map(deliveryDataDto -> orderService.findByOrderId(deliveryDataDto.getOrderId()))
          .collect(toList());
      if (!listOrders.isEmpty()) {
        listOrders.forEach(orderDto -> {
          deliveryDataService.updateStatus(orderDto.getDeliveryStatus(), orderDto.getOrderId());
        });
        log.info("update delivery status completed!");
      } else {
        log.info("updateDeliveryStatus: Nothing to update");
      }
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
  }
}
